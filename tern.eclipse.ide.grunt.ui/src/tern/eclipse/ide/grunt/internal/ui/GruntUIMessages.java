/**
 *  Copyright (c) 2015 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package tern.eclipse.ide.grunt.internal.ui;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;

/**
 * Grunt UI messages.
 * 
 */
public final class GruntUIMessages extends NLS {

	private static final String BUNDLE_NAME = "tern.eclipse.ide.grunt.internal.ui.GruntUIMessages"; //$NON-NLS-1$

	private static ResourceBundle fResourceBundle;

	// Grunt explorer
	public static String RefreshExplorerAction_text;
	public static String RefreshExplorerAction_tooltip;
	public static String GoToDefinitionAction_text;
	public static String GoToDefinitionAction_tooltip;
	public static String GruntView_openFile_error;
	public static String GruntView_openFileDialog_title;

	private GruntUIMessages() {
	}

	public static ResourceBundle getResourceBundle() {
		try {
			if (fResourceBundle == null)
				fResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);
		} catch (MissingResourceException x) {
			fResourceBundle = null;
		}
		return fResourceBundle;
	}

	static {
		NLS.initializeMessages(BUNDLE_NAME, GruntUIMessages.class);
	}
}
