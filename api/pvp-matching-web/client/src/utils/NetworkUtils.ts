import axios, {AxiosError, AxiosResponse} from "axios";
import {message} from "antd";
import {isProd} from "./EnvConfig";

export async function sendGetRequest<T>(url: string, withCredentials: boolean): Promise<T | null> {
    try {
        const axiosResponse = await axios.get(url, {withCredentials: isProd() ? withCredentials : false});
        console.log("GET response data:", axiosResponse.data);

        const response = parseApiResponseData(axiosResponse);
        if (!response.success) {
            console.error("GET API response not successful:", response.error);
            return null;
        }

        // Handle case where message is a string that needs parsing
        if (typeof response.message === 'string') {
            try {
                return JSON.parse(response.message) as T;
            } catch (e) {
                console.error("Failed to parse message string as JSON:", e);
                return response.message as unknown as T;
            }
        }

        // Handle case where message might be null or undefined
        if (response.message === null || response.message === undefined) {
            console.warn("API response message is null or undefined");
            return null;
        }

        return response.message as T;
    } catch (e) {
        console.error("GET request failed:", e);
        if (e instanceof AxiosError) {
            console.error("Axios error details:", {
                status: e.response?.status,
                statusText: e.response?.statusText,
                data: e.response?.data,
                url: e.config?.url
            });
        }
        return null;
    }
}

export async function sendPostRequest<T>(url: string, data: {}, withCredentials: boolean): Promise<T> {
    console.log("POST request to:", url, "withCredentials:", withCredentials);
    console.log("POST data:", data);

    let response: IApiResponseData | undefined;
    try {
        const axiosResponse = await axios.post(url, data, {withCredentials: isProd() ? withCredentials : false});
        console.log("POST response data:", axiosResponse.data);
        response = parseApiResponseData(axiosResponse);
    } catch (e) {
        console.error("POST request failed:", e);
        if (e instanceof AxiosError) {
            console.error("Axios error details:", {
                status: e.response?.status,
                statusText: e.response?.statusText,
                data: e.response?.data,
                url: e.config?.url
            });
            response = parseApiResponseData(e.response as AxiosResponse);
        }
    }

    if (!response) {
        console.error("No response received");
        message.error('No response');
        throw new Error('No response');
    }

    if (!response.success || response.error) {
        console.error("API response error:", response.error);
        message.error(response.error);
        throw new Error(response.error);
    }

    if (typeof response.message == 'string') {
        message.error(JSON.parse(response.message));
        return JSON.parse(response.message);
    }
    return response.message as T;
}

function parseApiResponseData(response: AxiosResponse<any, any>) {
    return response.data as IApiResponseData;
}

interface IApiResponseData {
    success: boolean,
    error: string,
    message: any
}