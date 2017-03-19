/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script;

import php.java.bridge.http.IContext;
import php.java.bridge.http.IContextFactory;
import php.java.bridge.http.ISession;

/**
 * A custom context factory, creates a ContextFactory for JSR223 contexts.  sessions do not expire.
 * @author jostb
 *
 */
public class InteractivePhpScriptContextFactory extends PhpScriptContextFactory {

    /**
     * Add the PhpScriptContext
     * @param context
     * @return The ContextFactory.
     */
    public static IContextFactory addNew(IContext context) {
	InteractivePhpScriptContextFactory ctx = new InteractivePhpScriptContextFactory();
	ctx.setContext(context);
	return ctx;
    }
    /**{@inheritDoc}*/
    public ISession getSession(String name, short clientIsNew, int timeout) {
	// ignore timeout
	return super.getSession(name, clientIsNew, 0);
    }
}

