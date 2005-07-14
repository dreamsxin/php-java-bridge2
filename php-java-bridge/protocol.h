/*-*- mode: C; tab-width:4 -*-*/

#ifndef JAVA_PROTOCOL_H
#define JAVA_PROTOCOL_H

/* peer */
#include <stdio.h>
#ifdef __MINGW32__
# include <winsock2.h>
# define close closesocket
#else
# include <sys/types.h>
# include <sys/socket.h>
#endif

/* 
 * we create a unix domain socket with the name .php_java_bridge in
 * the tmpdir
 */
#ifndef P_tmpdir
/* xopen, normally defined in stdio.h */
#define P_tmpdir "/tmp"
#endif 
#define SOCKNAME P_tmpdir/**/"/.php_java_bridge"/**/"XXXXXX"

/*
 * default log file is System.out
 */
#define LOGFILE ""

#define LOG_OFF 0
#define LOG_FATAL 1
#define LOG_ERROR 2 /* default level */
#define LOG_INFO 3 
#define LOG_DEBUG 4
#define DEFAULT_LEVEL "2"

#define N_JAVA_SARGS 9
#define N_JAVA_SENV 3 
#define N_MONO_SARGS 5
#define N_MONO_SENV 1
#ifndef N_SARGS 
# define N_SARGS N_JAVA_SARGS	/* # of server args for exec */
#endif
#ifndef N_SENV
# define N_SENV N_JAVA_SENV		/* # of server env entries */
#endif
#define DEFAULT_MONO_PORT "9167" /* default port for tcp/ip */
#define DEFAULT_JAVA_PORT "9267" /* default port for tcp/ip */
#ifndef DEFAULT_PORT
# define DEFAULT_PORT DEFAULT_JAVA_PORT /* init_cfg.h overrides */
#endif
#define DEFAULT_HOST "127.0.0.1"
#define DEFAULT_SERVLET "/JavaBridge/PhpJavaServlet"

#define RECV_SIZE 8192 // initial size of the receive buffer
#define MAX_ARGS 100   // max # of method arguments

typedef struct proxyenv_ *proxyenv;
struct proxyenv_ {
  int peer;

  /* used by the parser implementation */
  unsigned char*s; size_t len; 
  ssize_t pos, c; 
  unsigned char recv_buf[RECV_SIZE];

  /* the send buffer */
  unsigned char*send;
  size_t send_len, send_size;

  char *server_name;

  /* local server (not a servlet engine) */
  short is_local;

  /* for servlets: re-open connection */
  short must_reopen; 

  /* the cookie, for servlet engines only */
  char *cookie_name, *cookie_value;
  
  void (*handle_request)(proxyenv *env);

  void (*writeCreateObjectBegin)(proxyenv *env, char*name, size_t strlen, char createInstance, void *result);
  void (*writeCreateObjectEnd)(proxyenv *env);
  void (*writeInvokeBegin)(proxyenv *env, long object, char*method, size_t strlen, char property, void* result);
  void (*writeInvokeEnd)(proxyenv *env);
  void (*writeResultBegin)(proxyenv *env, void* result);
  void (*writeResultEnd)(proxyenv *env);
  void (*writeGetMethodBegin)(proxyenv *env, long object, char*method, size_t strlen, void* result);
  void (*writeGetMethodEnd)(proxyenv *env);
  void (*writeCallMethodBegin)(proxyenv *env, long object, long method, void* result);
  void (*writeCallMethodEnd)(proxyenv *env);
  void (*writeString)(proxyenv *env, char*name, size_t strlen);
  void (*writeBoolean)(proxyenv *env, short boolean);
  void (*writeLong)(proxyenv *env, long l);
  void (*writeDouble)(proxyenv *env, double d);
  void (*writeObject)(proxyenv *env, long object);
  void (*writeCompositeBegin_a)(proxyenv *env);
  void (*writeCompositeBegin_h)(proxyenv *env);
  void (*writeCompositeEnd)(proxyenv *env);
  void (*writePairBegin_s)(proxyenv *env, char*key, size_t strlen);
  void (*writePairBegin_n)(proxyenv *env, unsigned long key);
  void (*writePairBegin)(proxyenv *env);
  void (*writePairEnd)(proxyenv *env);
  void (*writeUnref)(proxyenv *env, long object);
};

#endif