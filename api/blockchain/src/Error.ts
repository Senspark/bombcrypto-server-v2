import * as ErrorCode from './ErrorCode';

class BaseError extends Error {
  readonly httpCode?: number;
  readonly code?: number;

  constructor(message: string, httpCode?: number, code?: number) {
    super(message);
    this.httpCode = httpCode;
    this.code = code;
  }
}

class InvalidAddressError extends BaseError {
  constructor(address: string) {
    super(
      `Invalid address: ${address}`,
      400,
      ErrorCode.CODE_INVALID_ADDRESS,
    );
  }
}

class InvalidDateError extends BaseError {
  constructor(date: Date) {
    super(
      `Invalid date: ${date}`,
      400,
      ErrorCode.CODE_INVALID_DATE,
    );
  }
}

class InvalidBlockError extends BaseError {
  constructor(block: number) {
    super(
      `Invalid block: ${block}`,
      400,
      ErrorCode.CODE_INVALID_BLOCK,
    );
  }
}

class InvalidAuthorizationError extends BaseError {
  constructor(key: string) {
    super(
      `Invalid key: ${key}`,
      401,
      ErrorCode.CODE_INVALID_AUTHORIZATION,
    );
  }
}

class InternalError extends BaseError {
  constructor(data: {
    message: string,
    httpCode?: number,
    code?: number
  }) {
    super(
      data.message,
      data.httpCode,
      data.code ?? data.httpCode,
    );
  }
}

export {
  BaseError,
  InvalidAddressError,
  InvalidDateError,
  InvalidBlockError,
  InvalidAuthorizationError,
  InternalError,
};