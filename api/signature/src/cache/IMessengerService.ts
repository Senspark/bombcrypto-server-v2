export default interface IMessengerService {
  send(streamKey: string, message: any): Promise<boolean>;
}
