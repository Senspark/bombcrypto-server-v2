export default interface IMessengerService {
    /**
     * Redis Pub/Sub bus: one channel carries several message types, told apart by `type`.
     */
    publishBus(channel: string, type: string, message: any): Promise<boolean>;

    onBus(channel: string, type: string, callback: (message: any) => void): void;
}
