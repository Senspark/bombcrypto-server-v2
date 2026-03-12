export const RedisKeys = {
    AP_SOL_LOGIN_JWT_REFRESH: "AP:SOL:LOGIN:JWT_REFRESH_TOKEN",
    AP_SOL_LOGIN_PRIVATE_KEY: "AP:SOL:LOGIN:PRIVATE_KEY",
    AP_SOL_LOGIN_NONCE: "AP:SOL:LOGIN:NONCE",
    /**
     * Để chống dùng lại AES key 2 lần liên tiếp
     */
    AP_SOL_LOGIN_AES_USED: "AP:SOL:LOGIN:AES_USED",
    /**
     * Ban các user có động thái cheat rõ ràng
     */
    AP_SOL_LOGIN_BANNED: "AP:SOL:LOGIN:BANNED",

    AP_WEB_LOGIN_NONCE: "AP:WEB:LOGIN:NONCE",
    AP_WEB_LOGIN_JWT_REFRESH: "AP:WEB:LOGIN:JWT_REFRESH_TOKEN",
    AP_WEB_LOGIN_PRIVATE_KEY: "AP:WEB:LOGIN:PRIVATE_KEY",

    AP_RON_LOGIN_JWT_REFRESH: "AP:RON:LOGIN:JWT_REFRESH_TOKEN",
    AP_RON_LOGIN_PRIVATE_KEY: "AP:RON:LOGIN:PRIVATE_KEY",
    AP_RON_LOGIN_NONCE: "AP:RON:LOGIN:NONCE",

    AP_BAS_LOGIN_JWT_REFRESH: "AP:BAS:LOGIN:JWT_REFRESH_TOKEN",
    AP_BAS_LOGIN_PRIVATE_KEY: "AP:BAS:LOGIN:PRIVATE_KEY",
    AP_BAS_LOGIN_NONCE: "AP:BAS:LOGIN:NONCE",

    AP_VIC_LOGIN_JWT_REFRESH: "AP:VIC:LOGIN:JWT_REFRESH_TOKEN",
    AP_VIC_LOGIN_PRIVATE_KEY: "AP:VIC:LOGIN:PRIVATE_KEY",
    AP_VIC_LOGIN_NONCE: "AP:VIC:LOGIN:NONCE",

    AP_BSC_LOGIN_JWT_REFRESH: "AP:BSC:LOGIN:JWT_REFRESH_TOKEN",
    AP_BSC_LOGIN_PRIVATE_KEY: "AP:BSC:LOGIN:PRIVATE_KEY",
    AP_BSC_LOGIN_NONCE: "AP:BSC:LOGIN:NONCE",

    AP_POL_LOGIN_JWT_REFRESH: "AP:POL:LOGIN:JWT_REFRESH_TOKEN",
    AP_POL_LOGIN_PRIVATE_KEY: "AP:POL:LOGIN:PRIVATE_KEY",
    AP_POL_LOGIN_NONCE: "AP:POL:LOGIN:NONCE",

    AP_TON_LOGIN_JWT_REFRESH: "AP:TON:LOGIN:JWT_REFRESH_TOKEN",
    AP_TON_LOGIN_PRIVATE_KEY: "AP:TON:LOGIN:PRIVATE_KEY",

    AP_PASSWORD_RESET_TOKENS: "AP:WEB:PASSWORD_RESET_TOKENS",
}

export type CookieSettings = {
    clientToken: string;
    serverToken: string;
    refreshToken: string;
    dappClientToken: string;
    dappApiToken: string;
    domain: string;
    secure: boolean,
    sameSite: string,
}

export const getCookieSettings = (isProd: boolean, isCloud: boolean): CookieSettings => {
    const domain = isCloud ? '.bombcrypto.io' : 'localhost';
    const secure = isCloud;
    //const sameSite = isCloud ? 'strict' : 'none';
    const sameSite = 'none';

    const obj = {domain, secure, sameSite};

    if (isProd) {
        return {
            // Dùng cho client game (unity)
            clientToken: '_acl_au_1',
            serverToken: '_acl_au_2',
            refreshToken: '_acl_au_3',

            // Dùng cho client Dapp
            dappClientToken: '_acl_au_4',
            dappApiToken: '_acl_au_5',
            ...obj,
        }
    }
    return {
        // Dùng cho client game (unity)
        clientToken: '_bcl_au_1',
        serverToken: '_bcl_au_2',
        refreshToken: '_bcl_au_3',

        // Dùng cho client Dapp
        dappClientToken: '_bcl_au_4',
        dappApiToken: '_bcl_au_5',
        ...obj,
    }
}

export const RedisConfig = {
    K_REFRESH_TOKEN_EXPIRED: 3600 * 24 * 30, // 30 ngày
    K_PRIVATE_KEY_EXPIRED: 60 * 15, // 15 phút
    K_NONCE_EXPIRED: 60 * 5, // 5 phút
    K_SERVER_TOKEN_COOKIE_EXPIRED: 3600 * 24 * 365, // 1 years
    K_REFRESH_TOKEN_COOKIE_EXPIRED: 3600 * 24 * 30, // 30 ngày
    K_JWT_DAPP_COOKIE_EXPIRED: 3600 * 24 * 30, // 30 ngày
    K_AES_USED_EXPIRED: 60 * 5, // 5 phút
};

export const JwtExpired = {
    K_JWT_AUTH_EXPIRED: '30m', // 30 phút
    K_JWT_AUTH_DAPP_EXPIRED: '4weeks', // khoảng 1 tháng
    K_JWT_AUTH_DAPP_EXPIRED_TEST: '50m', // 5 phút
}

export type LoginData = {
    aesKey: string,
    encryptedJwt: string,
    extraData: string
}

export type UserInfoData = {
    aesKey: string,
    extraData: string,
}

export type JwtTokenInfo = {
    jwt: string
    refreshToken: string,
    walletAddress: string
}

export type JwtPayload = {
    address: string;
};
// userName can be wallet address or account name
export type JwtDapp = {
    userName: string;
};

export type JwtPayloadAccount = {
    userName: string;
};

export type JwtPayloadGuest = {
    userName: string;
};

export type JwtResponseData = {
    auth: string; // jwt
    rf?: string; // refresh token
    key: string; // rsa public key
    extraData: string
}
