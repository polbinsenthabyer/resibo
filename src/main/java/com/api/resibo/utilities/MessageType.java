package com.api.resibo.utilities;

/**
 * Response message constants. Add entries as modules are built, rather than
 * inlining message strings at call sites.
 */
public class MessageType {

    // --- health ---------------------------------------------------------
    public static final String API_HEALTH_CHECK = "Resibo API is healthy!";

    // --- generic --------------------------------------------------------
    public static final String ERROR_DTO_IS_NULL_OR_INCOMPLETE = "ERROR DTO IS NULL OR INCOMPLETE";
    public static final String ERROR_SOMETHING_WENT_WRONG = "ERROR SOMETHING WENT WRONG";
    public static final String SUCCESSFULLY_FETCHED_DATA = "SUCCESSFULLY FETCHED DATA";

    // --- security -------------------------------------------------------
    public static final String ERROR_UNAUTHENTICATED = "ERROR AUTHENTICATION REQUIRED";
    public static final String ERROR_NO_PERMISSION = "ERROR YOU DO NOT HAVE PERMISSION";

    // --- auth -----------------------------------------------------------
    public static final String ERROR_USERNAME_REQUIRED = "ERROR USERNAME IS REQUIRED";
    public static final String ERROR_USERNAME_TOO_LONG = "ERROR USERNAME EXCEEDS 100 CHARACTERS";
    public static final String ERROR_PASSWORD_TOO_SHORT = "ERROR PASSWORD MUST BE AT LEAST 8 CHARACTERS";
    public static final String ERROR_USERNAME_ALREADY_EXISTS = "ERROR USERNAME ALREADY EXISTS";
    public static final String ERROR_INVALID_ROLE = "ERROR INVALID ROLE";
    /** Deliberately identical for unknown user, wrong password and disabled account. */
    public static final String ERROR_INVALID_CREDENTIALS = "ERROR INVALID CREDENTIALS";
    public static final String SUCCESSFULLY_SIGNED_UP = "SUCCESSFULLY SIGNED UP";
    public static final String SUCCESSFULLY_LOGGED_IN = "SUCCESSFULLY LOGGED IN";

    // --- product --------------------------------------------------------
    public static final String ERROR_PRODUCT_NAME_REQUIRED = "ERROR PRODUCT NAME IS REQUIRED";
    public static final String ERROR_PRODUCT_NAME_TOO_LONG = "ERROR PRODUCT NAME EXCEEDS 100 CHARACTERS";
    public static final String ERROR_PRODUCT_DESCRIPTION_TOO_LONG = "ERROR PRODUCT DESCRIPTION EXCEEDS 250 CHARACTERS";
    public static final String ERROR_PRODUCT_PRICE_NEGATIVE = "ERROR PRODUCT PRICE CANNOT BE NEGATIVE";
    public static final String ERROR_PRODUCT_STOCKS_NEGATIVE = "ERROR PRODUCT REMAINING STOCKS CANNOT BE NEGATIVE";
    public static final String ERROR_PRODUCT_NOT_EXIST = "ERROR PRODUCT DOES NOT EXIST";
    public static final String ERROR_PRODUCT_NAME_ALREADY_EXISTS = "ERROR PRODUCT NAME ALREADY EXISTS";
    public static final String SUCCESSFULLY_SET_PRODUCT = "SUCCESSFULLY SET PRODUCT";
    public static final String SUCCESSFULLY_DELETED_PRODUCT = "SUCCESSFULLY DELETED PRODUCT";

    private MessageType() {
        // constants only
    }
}
