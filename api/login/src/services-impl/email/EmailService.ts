import nodemailer from 'nodemailer';
import {EmailOptions, EmailServiceConfig, IEmailService} from "../../services/IEmailService";
import {ILogger} from "../../Services";

export class EmailService implements IEmailService {
    private transporter?: nodemailer.Transporter;
    private readonly _config: EmailServiceConfig;
    private readonly _logger: ILogger;

    constructor(config: EmailServiceConfig, logger: ILogger) {
        this._config = config;
        this._logger = logger.clone("[EMAIL]");

        try {
            const transporter = nodemailer.createTransport({
                host: config.host,
                port: config.port,
                secure: config.secure,
                auth: {
                    user: config.auth.user,
                    pass: config.auth.pass,
                },
            });

            transporter.verify().then(() => {
                this.transporter = transporter;
            }).catch((error) => {
                this._logger.errors(`EmailService transporter verify error`, error);
            });
        } catch (error) {
            this._logger.error(`Failed to initialize email service: ${error.message}`);
        }
    }

    sendEmail(to: string, html: string): Promise<boolean> {
        return this.sendEmailWithOpt({
            to,
            subject: '[Senspark] Reset password account',
            text: '',
            html: html,
        });
    }

    /**
     * Sends an email to a single recipient
     * @param options Email options including recipient, subject, content
     * @returns Promise resolving to true if the email was sent, or an error if it failed
     */
    async sendEmailWithOpt(options: EmailOptions): Promise<boolean> {
        if (!this.transporter) {
            throw new Error('Email service not initialized. Call initialize() first.');
        }

        try {
            const result = await this.transporter.sendMail({
                from: this._config.from,
                to: options.to,
                subject: options.subject,
                text: options.text,
                html: options.html,
                attachments: options.attachments?.map(attachment => ({
                    filename: attachment.filename,
                    content: attachment.content,
                    contentType: attachment.contentType
                })),
            });

            return !!result.messageId;
        } catch (error) {
            this._logger.error(`Failed to send email: ${error.message}`);
            throw new Error(`Failed to send email: ${error.message}`);
        }
    }
}
