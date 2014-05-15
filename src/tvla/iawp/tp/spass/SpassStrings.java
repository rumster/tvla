package tvla.iawp.tp.spass;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class SpassStrings {

	private static final String BUNDLE_NAME = "tvla.iawp.tp.spass.spass"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private SpassStrings() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
