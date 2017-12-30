package test.php.java.servlet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import javax.servlet.ServletContext;

import org.junit.Test;

import php.java.servlet.ServletUtil;

public class TestServletUtil {
    
    private ServletContext ctx = mock(ServletContext.class);
    
    @Test(expected = IllegalStateException.class)
    public void testGetRealPathCannotDealWithRootDirectoryMissing() throws Exception {
	when(ctx.getRealPath(anyString())).thenReturn(null);
	when(ctx.getResource(anyString())).thenReturn(null);

	ServletUtil.getRealPath(ctx, "bleh");
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRealPathCannotDealWithNonFileResources() throws Exception {
	when(ctx.getRealPath(anyString())).thenReturn(null);
	when(ctx.getResource(anyString())).thenReturn(new URL("http://foo.org"));

	ServletUtil.getRealPath(ctx, "blah");

    }
   
    @Test
    public void testGetRealPath() throws Exception {
	when(ctx.getRealPath(anyString())).thenReturn(null);
	when(ctx.getResource(anyString())).thenReturn(new URL("file:c:/foo"));
	
	assertThat(ServletUtil.getRealPath(ctx, ""), is("c:\\foo\\"));
	assertThat(ServletUtil.getRealPath(ctx, "/bar"), is("c:\\foo\\bar"));
	assertThat(ServletUtil.getRealPath(ctx, "bar"), is("c:\\foo\\bar"));
    }
     
    @Test
    public void testGetRealPathCached() throws Exception {
	when(ctx.getRealPath(anyString())).thenReturn(null);
	when(ctx.getResource(anyString())).thenReturn(new URL("file:c:/foo"));
	assertThat(ServletUtil.getRealPath(ctx, ""), is("c:\\foo\\"));

	when(ctx.getResource(anyString())).thenReturn(null);
	assertThat(ServletUtil.getRealPath(ctx, ""), is("c:\\foo\\"));

    }

}
