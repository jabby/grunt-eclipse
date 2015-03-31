package tern.eclipse.ide.grunt.core.launch;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;

public class LaunchUtil {

	public static String generateConfigName(String baseName) {
		return getLaunchManager().generateLaunchConfigurationName(baseName);
	}

	public static ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
}
