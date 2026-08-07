/**
 * Standalone foundation layer for the ledger-engine service.
 *
 * <pre>
 * response/     R, Result, Response, SystemResponse, Pagination, PaginationDto
 * exception/    BizException, BaseGlobalExceptionHandler
 * entity/       AuditEntity, AuditEntityWithIsActive, BaseResponseDto
 * common/       AppContext, AppContextHolder, request/log metadata
 * api/          ApiClient
 * utils/        JSONUtil, EndpointHandler, CallableUtil, ExceptionUtil
 * aop/log/      LogScope
 * constant/enu/ LogType, Currency, CurrencyType
 * kafka/        BaseEvent, BaseListener
 * </pre>
 *
 * Ledger-specific mapped superclasses: {@code entity.TenancyAware}, {@code entity.WalletIdAware}.
 */
package com.altech.core;
