export interface ISuccessResponseSchema extends IResponseSchema {
    status: 'success';
    data: {};
    /**
     * Optional message providing additional information
     */
    message: string | null;
}

export interface IErrorResponseSchema extends IResponseSchema {
    status: 'error';
    error: {
        /**
         * HTTP response code
         */
        code: number;
        /**
         * Description of the error
         */
        message: string;
    };
}

interface IResponseSchema {
    status: 'success' | 'error';
}