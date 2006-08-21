/*-*- mode: Java; tab-width:8 -*-*/

package php.java.faces;

/*
 * Copyright (C) 2006 Jost Boekemeier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER(S) OR AUTHOR(S) BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;
import javax.faces.el.PropertyResolver;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.ValueBinding;
import javax.faces.el.VariableResolver;
import javax.faces.event.ActionListener;
import javax.faces.validator.Validator;

/**
 * A custom Application, overrides getPropertyResolver.
 * @author jostb
 * @see php.java.faces.PhpFacesPropertyResolver
 */
public class PhpFacesApplication extends Application {


    private Application app;
    public PhpFacesApplication(Application app) {
	this.app = app;
    }
    /* (non-Javadoc)
     * @see javax.faces.context.Application#getActionListener()
     */
    public ActionListener getActionListener() {
	return app.getActionListener();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setActionListener(javax.faces.event.ActionListener)
     */
    public void setActionListener(ActionListener listener) {
	app.setActionListener(listener);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getDefaultLocale()
     */
    public Locale getDefaultLocale() {
	return app.getDefaultLocale();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setDefaultLocale(java.util.Locale)
     */
    public void setDefaultLocale(Locale locale) {
	app.setDefaultLocale(locale);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getDefaultRenderKitId()
     */
    public String getDefaultRenderKitId() {
	return app.getDefaultRenderKitId();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setDefaultRenderKitId(java.lang.String)
     */
    public void setDefaultRenderKitId(String renderKitId) {
	app.setDefaultRenderKitId(renderKitId);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getMessageBundle()
     */
    public String getMessageBundle() {
	return app.getMessageBundle();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setMessageBundle(java.lang.String)
     */
    public void setMessageBundle(String bundle) {
	app.setMessageBundle(bundle);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getNavigationHandler()
     */
    public NavigationHandler getNavigationHandler() {
	return app.getNavigationHandler();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setNavigationHandler(javax.faces.context.NavigationHandler)
     */
    public void setNavigationHandler(NavigationHandler handler) {
	app.setNavigationHandler(handler);
    }

    private PropertyResolver resolver = null;
    PropertyResolver getPropertyResolver(PropertyResolver delegate) {
        if(resolver!=null) return resolver;
        return resolver = new PhpFacesPropertyResolver(delegate);
    }
    /* (non-Javadoc)
     * @see javax.faces.context.Application#getPropertyResolver()
     */
    public PropertyResolver getPropertyResolver() {
       return getPropertyResolver(app.getPropertyResolver());
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setPropertyResolver(javax.faces.el.PropertyResolver)
     */
    public void setPropertyResolver(PropertyResolver resolver) {
	if(!(resolver instanceof PhpFacesPropertyResolver)) app.setPropertyResolver(getPropertyResolver(resolver));
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getVariableResolver()
     */
    public VariableResolver getVariableResolver() {
	return app.getVariableResolver();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setVariableResolver(javax.faces.el.VariableResolver)
     */
    public void setVariableResolver(VariableResolver resolver) {
	app.setVariableResolver(resolver);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getViewHandler()
     */
    public ViewHandler getViewHandler() {
	return app.getViewHandler();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setViewHandler(javax.faces.context.ViewHandler)
     */
    public void setViewHandler(ViewHandler handler) {
	app.setViewHandler(handler);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getStateManager()
     */
    public StateManager getStateManager() {
	return app.getStateManager();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setStateManager(javax.faces.context.StateManager)
     */
    public void setStateManager(StateManager manager) {
	app.setStateManager(manager);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#addComponent(java.lang.String, java.lang.String)
     */
    public void addComponent(String componentType, String componentClass) {
	app.addComponent(componentType, componentClass);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#createComponent(java.lang.String)
     */
    public UIComponent createComponent(String componentType)
	throws FacesException {
	return app.createComponent(componentType);
    }


    /* (non-Javadoc)
     * @see javax.faces.context.Application#getComponentTypes()
     */
    public Iterator getComponentTypes() {
	return app.getComponentTypes();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#addConverter(java.lang.String, java.lang.String)
     */
    public void addConverter(String converterId, String converterClass) {
	app.addConverter(converterId, converterClass);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#addConverter(java.lang.Class, java.lang.String)
     */
    public void addConverter(Class targetClass, String converterClass) {
	app.addConverter(targetClass, converterClass);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#createConverter(java.lang.String)
     */
    public Converter createConverter(String converterId) {
	return app.createConverter(converterId);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#createConverter(java.lang.Class)
     */
    public Converter createConverter(Class targetClass) {
	return app.createConverter(targetClass);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getConverterIds()
     */
    public Iterator getConverterIds() {
	return app.getConverterIds();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getConverterTypes()
     */
    public Iterator getConverterTypes() {
	return app.getConverterTypes();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#createMethodBinding(java.lang.String, java.lang.Class[])
     */
    public MethodBinding createMethodBinding(String ref, Class[] params)
	throws ReferenceSyntaxException {
	return new PhpFacesMethodBindingImpl(this, ref, params);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getSupportedLocales()
     */
    public Iterator getSupportedLocales() {
	return app.getSupportedLocales();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#setSupportedLocales(java.util.Collection)
     */
    public void setSupportedLocales(Collection locales) {
	app.setSupportedLocales(locales);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#addValidator(java.lang.String, java.lang.String)
     */
    public void addValidator(String validatorId, String validatorClass) {
	app.addValidator(validatorId, validatorClass);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#createValidator(java.lang.String)
     */
    public Validator createValidator(String validatorId)
	throws FacesException {
	return app.createValidator(validatorId);
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#getValidatorIds()
     */
    public Iterator getValidatorIds() {
	return app.getValidatorIds();
    }

    /* (non-Javadoc)
     * @see javax.faces.context.Application#createValueBinding(java.lang.String)
     */
    public ValueBinding createValueBinding(String ref)
	throws ReferenceSyntaxException {
	return app.createValueBinding(ref);
    }
    
    /* (non-Javadoc)
     * @see javax.faces.application.Application#createComponent(javax.faces.el.ValueBinding, javax.faces.context.FacesContext, java.lang.String)
     */
    public UIComponent createComponent(ValueBinding componentBinding, FacesContext context, String componentType) throws FacesException {
        return app.createComponent(componentBinding, context, componentType);
    }

}
