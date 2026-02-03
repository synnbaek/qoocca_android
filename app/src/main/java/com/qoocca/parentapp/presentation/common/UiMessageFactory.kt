package com.qoocca.parentapp.presentation.common

import com.qoocca.parentapp.data.network.ApiResult

object UiMessageFactory {
    const val PHONE_REQUIRED = "전화번호를 입력해주세요."
    const val TOKEN_REQUIRED = "로그인 토큰을 찾을 수 없습니다. 다시 로그인해주세요."
    const val LOGIN_FAILED = "로그인 실패 (번호 확인)"
    const val SERVER_CONNECTION_FAILED = "서버 연결 실패"
    const val BAD_SERVER_RESPONSE = "잘못된 서버 응답"
    const val RECEIPT_PARSE_ERROR = "데이터를 처리하는 중 오류가 발생했습니다."
    const val AUTH_FAILED = "인증에 실패했습니다. 다시 로그인 해주세요."
    const val RECEIPT_NOT_FOUND = "결제 정보를 찾을 수 없습니다."
    const val RECEIPT_FETCH_FAILED = "결제 정보를 불러오는데 실패했습니다."
    const val NOTHING_TO_PAY = "결제할 항목이 없습니다."
    const val PAYMENT_SUCCESS = "결제가 성공적으로 완료되었습니다."

    fun loginFailure(result: ApiResult<*>): String {
        return when (result) {
            is ApiResult.HttpError -> LOGIN_FAILED
            is ApiResult.NetworkError -> SERVER_CONNECTION_FAILED
            is ApiResult.UnknownError -> BAD_SERVER_RESPONSE
            is ApiResult.Success -> BAD_SERVER_RESPONSE
        }
    }

    fun receiptListFailure(result: ApiResult<*>): String {
        return when (result) {
            is ApiResult.NetworkError -> SERVER_CONNECTION_FAILED
            is ApiResult.UnknownError -> RECEIPT_PARSE_ERROR
            is ApiResult.HttpError -> {
                if (result.code == 403) {
                    AUTH_FAILED
                } else {
                    "결제 목록을 가져오지 못했습니다. (코드: ${result.code})"
                }
            }
            is ApiResult.Success -> RECEIPT_PARSE_ERROR
        }
    }

    fun paymentFailedCount(failedCount: Int): String {
        return "${failedCount}건의 결제에 실패했습니다. 다시 시도해주세요."
    }
}
