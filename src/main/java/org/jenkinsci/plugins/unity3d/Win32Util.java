package org.jenkinsci.plugins.unity3d;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeMapped;
import com.sun.jna.PointerType;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import java.util.HashMap;
import java.util.Map;

public class Win32Util {

    static String getLocalAppData() {
        HWND hwndOwner = null;
        int nFolder = Shell32.CSIDL_LOCAL_APPDATA;
        HANDLE hToken = null;
        int dwFlags = Shell32.SHGFP_TYPE_CURRENT;
        char[] pszPath = new char[Shell32.MAX_PATH];
        int hResult = Shell32.INSTANCE.SHGetFolderPath(hwndOwner, nFolder, hToken, dwFlags, pszPath);
        if (Shell32.S_OK == hResult) {
            String path = new String(pszPath);
            int len = path.indexOf('\0');
            return path.substring(0, len);
        } else {
            throw new RuntimeException("Unable to find PATH under CSIDL_LOCAL_APPDATA [" + hResult + "]");
        }
    }

    private static Map<String, Object> OPTIONS = new HashMap<>();

    static {
        OPTIONS.put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
        OPTIONS.put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
    }

    static class HANDLE extends PointerType implements NativeMapped {}

    static class HWND extends HANDLE {}

    interface Shell32 extends Library {
        int MAX_PATH = 260;
        int CSIDL_LOCAL_APPDATA = 0x001c;
        int SHGFP_TYPE_CURRENT = 0;
        // public static final int SHGFP_TYPE_DEFAULT = 1;
        int S_OK = 0;

        Shell32 INSTANCE = Native.load("shell32", Shell32.class, OPTIONS);

        /**
         * see http://msdn.microsoft.com/en-us/library/bb762181(VS.85).aspx
         *
         * HRESULT SHGetFolderPath( HWND hwndOwner, int nFolder, HANDLE hToken,
         * DWORD dwFlags, LPTSTR pszPath);
         */
        int SHGetFolderPath(HWND hwndOwner, int nFolder, HANDLE hToken, int dwFlags, char[] pszPath);
    }
}
