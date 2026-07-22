export default interface IMessengerService {
    send(streamKey: string, message: any): Promise<boolean>;

    listen(streamKey: string, callback: (message: any) => void): void;
}