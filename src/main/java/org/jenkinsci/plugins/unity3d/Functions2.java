package org.jenkinsci.plugins.unity3d;

class Functions2 {
	public static boolean isMac() {
		return IS_OS_MAC;
	}
	public static boolean isLinux() {
		return IS_OS_LINUX;
	}
    private static final boolean IS_OS_LINUX = getOSMatchesName("Linux") || getOSMatchesName("LINUX");
    private static final boolean IS_OS_MAC = getOSMatchesName("Mac");

    private static final String OS_NAME = System.getProperty("os.name");

    private static boolean getOSMatchesName(String osNamePrefix) {
        return isOSNameMatch(OS_NAME, osNamePrefix);
    }
    static boolean isOSNameMatch(String osName, String osNamePrefix) {
        if (osName == null) {
            return false;
        }
        return osName.startsWith(osNamePrefix);
    }
}