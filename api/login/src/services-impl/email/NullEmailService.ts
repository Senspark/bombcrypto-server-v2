import {EmailOptions, IEmailService} from "../../services/IEmailService";

export class NullEmailService implements IEmailService {
    sendEmail(to: string, html: string): Promise<boolean> {
        return Promise.resolve(false);
    }

    sendEmailWithOpt(options: EmailOptions): Promise<boolean> {
        return Promise.resolve(false);
    }
}