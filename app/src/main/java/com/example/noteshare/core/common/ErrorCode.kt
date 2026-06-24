package com.example.noteshare.core.common

object ErrorCode {
    const val SUCCESS = 0
    const val PARAM_INVALID = 40000
    const val USERNAME_EXISTS = 40010
    const val USERNAME_FORMAT_INVALID = 40011
    const val PASSWORD_LENGTH_INVALID = 40012
    const val LOGIN_FAILED = 40020
    const val AUTH_TOKEN_MISSING = 40100
    const val AUTH_TOKEN_INVALID = 40101
    const val AUTH_TOKEN_EXPIRED = 40102
    const val NOT_LOGGED_IN = 40103
    const val FORBIDDEN = 40104
    const val USER_NOT_FOUND = 40200
    const val CANNOT_FOLLOW_SELF = 40210
    const val ALREADY_FOLLOWING = 40211
    const val NOT_FOLLOWING = 40212
    const val NOTE_NOT_FOUND = 40300
    const val NOTE_FORBIDDEN = 40301
    const val NOTE_TITLE_REQUIRED = 40310
    const val NOTE_TITLE_TOO_LONG = 40311
    const val NOTE_CONTENT_REQUIRED = 40312
    const val ALREADY_LIKED = 40400
    const val NOT_LIKED = 40401
    const val COMMENT_EMPTY = 40500
    const val COMMENT_NOT_FOUND = 40501
    const val COMMENT_FORBIDDEN = 40502
    const val COMMENT_PARENT_NOT_FOUND = 40503
    const val COMMENT_LIKE_ALREADY = 40510
    const val COMMENT_LIKE_NOT_FOUND = 40511
    const val FILE_EMPTY = 40600
    const val FILE_TYPE_INVALID = 40601
    const val FILE_TOO_LARGE = 40602
    const val FILE_UPLOAD_FAILED = 40603
    const val VIDEO_TYPE_NOT_ALLOWED = 40604
    const val VIDEO_TOO_LARGE = 40605
    const val SERVER_ERROR = 50000

    const val NETWORK_ERROR = -1
    const val INVALID_PARAMETER = -2
}
