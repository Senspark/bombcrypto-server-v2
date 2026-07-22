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

class BadRequestError extends BaseError {
  constructor(message: string) {
    super(message, 400, ErrorCode.CODE_BAD_REQUEST);
  }
}

class InvalidAddressError extends BaseError {
  constructor(address: string) {
    super(`Invalid address: ${address}`, 400, ErrorCode.CODE_INVALID_ADDRESS);
  }
}

class InvalidAuthorizationError extends BaseError {
  constructor(message: string = 'Invalid authorization') {
    super(message, 401, ErrorCode.CODE_INVALID_AUTHORIZATION);
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
  BadRequestError,
  InvalidAddressError,
  InvalidAuthorizationError,
  InternalError,
};
