/*-*- mode: C; tab-width:4 -*-*/

#include <stdlib.h>
#include "php.h"
#include "php_java.h"
#include "java_bridge.h"
#include <jni.h>

/* miscellaneous */
#include <stdio.h>
#include <assert.h>

/* strings */
#include <string.h>


ZEND_DECLARE_MODULE_GLOBALS(java)

static int checkError(pval *value);
static jobjectArray php_java_makeArray(int argc, pval** argv TSRMLS_DC);
static jobject php_java_makeObject(pval* arg TSRMLS_DC);


void php_java_call_function_handler(INTERNAL_FUNCTION_PARAMETERS, char*name, pval *object, int arg_count, zval**arguments)
{
  proxyenv *jenv;
  jlong result = 0;

  /* check if we're initialized */
  jenv = JG(jenv);
  if(!jenv) {
	php_error(E_ERROR, "java not initialized");
	return;
  }

  if (!strcmp("java", name)) {
    /* construct a Java object:
       First argument is the class name.  Any additional arguments will
       be treated as constructor parameters. */
    jmethodID co = (*jenv)->GetMethodID(jenv, JG(reflect_class), "CreateObject",
      "(Ljava/lang/String;[Ljava/lang/Object;JJ)V");
    jstring className;
    result = (jlong)(long)object;

    if (ZEND_NUM_ARGS() < 1) {
      php_error(E_ERROR, "Missing classname in new java() call");
      return;
    }

    className=(*jenv)->NewStringUTF(jenv, Z_STRVAL_P(arguments[0]));
	assert(className);
	/* create a new object */
	(*jenv)->CreateObject(jenv, JG(php_reflect), co,
      className, php_java_makeArray(arg_count-1, arguments+1 TSRMLS_CC), result);

    (*jenv)->DeleteLocalRef(jenv, className);

  } else {

    pval **handle;
    int type;
    jobject obj;
    jstring method;


    jmethodID invoke = (*jenv)->GetMethodID(jenv, JG(reflect_class), "Invoke",
      "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;JJ)V");
    zend_hash_index_find(Z_OBJPROP_P(object), 0, (void**) &handle);
    obj = zend_list_find(Z_LVAL_PP(handle), &type);
    method = (*jenv)->NewStringUTF(jenv, name);
    result = (jlong)(long)return_value;
    /* invoke a method on the given object */
    (*jenv)->Invoke(jenv, JG(php_reflect), invoke,
      obj, method, php_java_makeArray(arg_count, arguments TSRMLS_CC), result);

    (*jenv)->DeleteLocalRef(jenv, method);
  }
  checkError((pval*)(long)result);
}

static jobject php_java_makeObject(pval* arg TSRMLS_DC)
{
  proxyenv *jenv = JG(jenv);
  jobject result;
  pval **handle;
  int type;
  jmethodID makeArg;
  jclass hashClass;

  switch (Z_TYPE_P(arg)) {
    case IS_STRING:
      result=(*jenv)->NewByteArray(jenv, Z_STRLEN_P(arg));
      (*jenv)->SetByteArrayRegion(jenv, (jbyteArray)result, 0,
        Z_STRLEN_P(arg), Z_STRVAL_P(arg));
      break;

    case IS_OBJECT:
      zend_hash_index_find(Z_OBJPROP_P(arg), 0, (void*)&handle);
      result = zend_list_find(Z_LVAL_PP(handle), &type);
      break;

    case IS_BOOL:
      makeArg = (*jenv)->GetMethodID(jenv, JG(reflect_class), "MakeArg",
        "(Z)Ljava/lang/Object;");
      result = (*jenv)->CallObjectMethod(1, jenv, JG(php_reflect), makeArg, 
										 (jboolean)(Z_LVAL_P(arg)));
      break;

    case IS_LONG:
      makeArg = (*jenv)->GetMethodID(jenv, JG(reflect_class), "MakeArg",
        "(J)Ljava/lang/Object;");
      result = (*jenv)->CallObjectMethod(1, jenv, JG(php_reflect), makeArg, 
										 (jlong)(Z_LVAL_P(arg)));
      break;

    case IS_DOUBLE:
      makeArg = (*jenv)->GetMethodID(jenv, JG(reflect_class), "MakeArg",
        "(D)Ljava/lang/Object;");
      result = (*jenv)->CallObjectMethod(1, jenv, JG(php_reflect), makeArg,
        (jdouble)(Z_DVAL_P(arg)));
      break;

    case IS_ARRAY:
      {
      jobject jkey, jval;
      zval **value;
      zval key;
      char *string_key;
      ulong num_key;
      jobject jold;
      jmethodID put, init;

      hashClass = (*jenv)->FindClass(jenv, "java/util/Hashtable");
      init = (*jenv)->GetMethodID(jenv, hashClass, "<init>", "()V");
      result = (*jenv)->NewObject(0, jenv, hashClass, init);

      put = (*jenv)->GetMethodID(jenv, hashClass, "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

      /* Iterate through hash */
      zend_hash_internal_pointer_reset(Z_ARRVAL_P(arg));
      while(zend_hash_get_current_data(Z_ARRVAL_P(arg), (void**)&value) == SUCCESS) {
        jval = php_java_makeObject(*value TSRMLS_CC);

        switch (zend_hash_get_current_key(Z_ARRVAL_P(arg), &string_key, &num_key, 0)) {
          case HASH_KEY_IS_STRING:
            Z_TYPE(key) = IS_STRING;
            Z_STRVAL(key) = string_key;
            Z_STRLEN(key) = strlen(string_key);
            jkey = php_java_makeObject(&key TSRMLS_CC);
            break;
          case HASH_KEY_IS_LONG:
            Z_TYPE(key) = IS_LONG;
            Z_LVAL(key) = num_key;
            jkey = php_java_makeObject(&key TSRMLS_CC);
            break;
          default: /* HASH_KEY_NON_EXISTANT */
            jkey = 0;
        }
        jold = (*jenv)->CallObjectMethod(2, jenv, result, put, 
										 jkey, jval);
        if (Z_TYPE_PP(value) != IS_OBJECT) (*jenv)->DeleteLocalRef(jenv, jval);
        zend_hash_move_forward(Z_ARRVAL_P(arg));
      }

      break;
      }

    default:
      result=0;
  }

  return result;
}

static jobjectArray php_java_makeArray(int argc, pval** argv TSRMLS_DC)
{
  jobjectArray result;
  jobject arg;
  int i;
  proxyenv *jenv = JG(jenv);

  jclass objectClass = (*jenv)->FindClass(jenv, "java/lang/Object");
  assert(objectClass);
  result = (*jenv)->NewObjectArray(jenv, argc, objectClass, 0);
  assert(result);
  for (i=0; i<argc; i++) {
    arg = php_java_makeObject(argv[i] TSRMLS_CC);
    (*jenv)->SetObjectArrayElement(jenv, result, i, arg);
    if (Z_TYPE_P(argv[i]) != IS_OBJECT) (*jenv)->DeleteLocalRef(jenv, arg);
  }
  return result;
}

static pval 
php_java_getset_property (char* name, pval* object, jobjectArray value TSRMLS_DC)
{
  pval presult;
  jlong result = 0;
  pval **pobject;
  jobject obj;
  int type;

  /* get the property name */
  jstring propName;

  proxyenv *jenv;
  jenv = JG(jenv);

  propName = (*jenv)->NewStringUTF(jenv, name);

  /* get the object */
  zend_hash_index_find(Z_OBJPROP_P(object),
    0, (void **) &pobject);
  obj = zend_list_find(Z_LVAL_PP(pobject), &type);
  result = (jlong)(long) &presult;
  Z_TYPE(presult) = IS_NULL;

  if (!obj || (type!=le_jobject)) {
    php_error(E_ERROR,
      "Attempt to access a Java property on a non-Java object");
  } else {
    /* invoke the method */
    jmethodID gsp = (*jenv)->GetMethodID(jenv, JG(reflect_class), "GetSetProp",
      "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;JJ)V");
    (*jenv)->GetSetProp(jenv, JG(php_reflect), gsp, obj, propName, value, result);
  }

  (*jenv)->DeleteLocalRef(jenv, propName);
  return presult;
}

static int checkError(pval *value)
{
  if (Z_TYPE_P(value) == IS_EXCEPTION) {
    php_error(E_WARNING, "%s", Z_STRVAL_P(value));
    efree(Z_STRVAL_P(value));
    ZVAL_FALSE(value);
    return 1;
  };
  return 0;
}

/**
 * php_java_get_property_handler
 */
pval php_java_get_property_handler(char*name, pval *object)
{
  pval presult = php_java_getset_property(name, object, 0 TSRMLS_CC);
  checkError(&presult);
  return presult;
}


/**
 * php_java_set_property_handler
 */
int php_java_set_property_handler(char*name, pval *object, pval *value)
{
  pval presult = 
	php_java_getset_property(name, object, php_java_makeArray(1, &value TSRMLS_CC) TSRMLS_CC);
  return checkError(&presult) ? FAILURE : SUCCESS;
}

/*
 * delete the object we've allocated during setResultFromObject
 */
void php_java_destructor(zend_rsrc_list_entry *rsrc TSRMLS_DC)
{
  // Disabled. In PHP 4 this was called *after* connection shutdown,
  // which is much too late.  The server part now does its own
  // resource tracking.
/* 	void *jobject = (void *)rsrc->ptr; */
/* 	assert(JG(jenv)); */
/* 	if (JG(jenv)) (*JG(jenv))->DeleteGlobalRef(JG(jenv), jobject); */
}
