package com.nexters.bandalart.core.domain.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

/**
 * runSuspendCatching
 *
 * @param block
 *
 * @return Result<T>
 *
 * 작업을 실행하고, 그 작업이 성공적으로 완료되면, 그 결과를 반환,
 * 그 작업에서 예외가 발생하면 그 예외를 캡쳐하는 역할을 수행
 */

// contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
// callInPlace: 계약의 종류 중 하나, callInPlace 계약은 이 함수가 람다를 'in-place'로, 즉 현재 위치에서 호출한다는 것을 나타냄
// 람다를 나중에 다른 곳에서 호출하지 않고, 람다를 받자마자 즉시 실행한다는 것을 의미

@OptIn(ExperimentalContracts::class)
internal inline fun <T> runSuspendCatching(block: () -> T): Result<T> {
    // Kotlin 의 contract(계약) 시스템을 이용해 block 이 정확히 한번만 호출 되어야 함을 나타냄
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return runCatching(block).also { result ->
        // 만약 람다에서 예외가 발생하면, Result 객체는 실패를 나타내고 해당 예외를 포함, 추가적인 작업을 실행
        val maybeException = result.exceptionOrNull()
        // 만약 예외가 CancellationException 이면 예외를 던져 코루틴 계층 구조에 따라 상위 코루틴까지 취소 신호를 전파
        // 이를 통해, 상위 코루틴에서 적절한 예외 처리 루틴을 수행할 수 있음
        if (maybeException is CancellationException) throw maybeException
    }
}
