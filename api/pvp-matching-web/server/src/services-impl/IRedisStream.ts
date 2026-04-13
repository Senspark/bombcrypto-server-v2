export interface IRedisStream {
    // connect(): Promise<void>;
    // disconnect(): Promise<void>;

    addToStream(streamKey: string, data: Record<string, string>): Promise<string>;

    /**
     * Đọc từ start đến start+count
     * @param streamKey
     * @param start
     * @param count
     */
    readStream(
        streamKey: string,
        start: string,
        count: number
    ): Promise<StreamMessage[]>;

    /**
     * Chờ đến khi nào có event mới
     * @param streamKey
     * @param lastId
     * @param timeout
     */
    readStreamBlocking(
        streamKey: string,
        lastId: string,
        timeout: number
    ): Promise<StreamMessage[]>;

    deleteFromStream(streamKey: string, id: string): Promise<number>;

    trimStream(streamKey: string, maxLength: number): Promise<number>;

    registerListener(
        streamKey: string,
        callback: MessageCallback,
        options: OnNewMessageOptions
    ): RemoveHandle;

    // unregisterListener(streamKey: string, callback: MessageCallback): void;
}

export interface StreamMessage {
    id: string;
    message: Record<string, string>;
}

export interface OnNewMessageOptions {
    startId?: string; // ID to start reading from
    interval?: number; // Maximum wait time (ms)
}

export type MessageCallback = (messages: StreamMessage[]) => void;
export type RemoveHandle = () => void;