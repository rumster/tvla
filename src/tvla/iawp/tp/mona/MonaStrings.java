package tvla.iawp.tp.mona;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class MonaStrings {

	private static final String BUNDLE_NAME = "tvla.iawp.tp.mona.mona"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private MonaStrings() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
