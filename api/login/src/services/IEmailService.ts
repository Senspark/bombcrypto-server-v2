export interface EmailAttachment {
    filename: string;
    content: Buffer | string;
    contentType?: string;
}

export interface EmailOptions {
    to: string;
    subject: string;
    text?: string;
    html?: string;
    attachments?: EmailAttachment[];
}

export interface IEmailService {
    /**
     * Sends an email to a single recipient
     * @param options Email options including recipient, subject, content
     * @returns Promise resolving to true if the email was sent, or an error if it failed
     */
    sendEmailWithOpt(options: EmailOptions): Promise<boolean>;

    sendEmail(to: string, html: string): Promise<boolean>;
}

export interface EmailServiceConfig {
    host: string;
    port: number;
    secure: boolean;
    auth: {
        user: string;
        pass: string;
    };
    from: string;
}
