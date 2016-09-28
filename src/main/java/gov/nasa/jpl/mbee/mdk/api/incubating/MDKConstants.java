package gov.nasa.jpl.mbee.mdk.api.incubating;

/**
 * Created by igomes on 9/26/16.
 */
public class MDKConstants {
    public static final String
            HIDDEN_ID_PREFIX = "_hidden_",
            HOLDING_BIN_PACKAGE_ID_REGEX = "^(holding_bin|(Y|M|D|H){2}_[0-9]+)_.+$",
            TYPE_KEY = "type",
            NAME_KEY = "name",
            SYNC_SYSML_ID_SUFFIX = "_sync",
            DERIVED_KEY_PREFIX = "_",
            ID_SUFFIX = "Id",
            ID_SUFFIX_PLURAL = ID_SUFFIX + "s",
            SYSML_ID_KEY = "sysml" + ID_SUFFIX;
}
