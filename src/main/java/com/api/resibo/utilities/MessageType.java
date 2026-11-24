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
