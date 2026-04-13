export function isProd() {
    return process.env.REACT_APP_IS_PROD === 'true';
}

export function getServerOrigin() {
    return process.env.REACT_APP_API_HOST || 'http://localhost:8000';
}
