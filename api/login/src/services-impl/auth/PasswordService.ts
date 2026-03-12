import {ILogger} from "../../Services";
import crypto from "crypto";
import {IDependencies} from "../../DependenciesInjector";
import {EmailServiceConfig, IEmailService} from "../../services/IEmailService";
import {EmailService} from "../email/EmailService";
import {RedisKeys} from "../../consts/Consts";
import {mailResetPassHTML} from "../email/resetPassword.html";
import {IRedisHash} from "../../services/IRedisDatabase";
import DatabaseAccess from "../DatabaseAccess";
import {NullEmailService} from "../email/NullEmailService";

export class PasswordService {
    constructor(
        private readonly _dep: IDependencies
    ) {
        this._logger = _dep.logger.clone('[PASSWORD]');
        this._redis = _dep.redis.hashes;
        this._databaseAccess = new DatabaseAccess(_dep.logger, _dep.databaseBackend);
        if (_dep.envConfig.mailSender) {
            const emailConfig = this.getEmailConfig(
                _dep.envConfig.mailSender,
                _dep.envConfig.mailPassword
            )
            this._email = new EmailService(emailConfig, this._logger);
        } else {
            this._email = new NullEmailService();
        }
    }

    private readonly _logger: ILogger;
    private readonly _databaseAccess: DatabaseAccess;
    private readonly _email: IEmailService;
    private readonly _redis: IRedisHash

    /**
     * Change password for a user by username
     * New password must be different from the current password
     * @param username Username of the account
     * @param currentPassword Current password for verification
     * @param newPassword New password to set
     * @returns Boolean indicating success or failure
     */
    public async changePassword(username: string, currentPassword: string, newPassword: string): Promise<boolean> {
        try {
            // Hash both the current and new passwords
            const shaCurrentPassword = crypto.createHash('sha512').update(currentPassword).digest('hex');
            const shaNewPassword = crypto.createHash('sha512').update(newPassword).digest('hex');

            return await this._databaseAccess.changePassword(username, shaCurrentPassword, shaNewPassword);
        } catch (e) {
            this._logger.error(`Error changing password: ${e.message}`);
            return false;
        }
    }

    /**
     * Force change password for a user fi, this user not need to verify current password
     * New password must be different from the current password
     * @param username Username of the account
     * @param newPassword New password to set
     * @returns Boolean indicating success or failure
     */
    public async forceChangePassword(username: string, newPassword: string): Promise<boolean> {
        try {
            // Hash the new password
            const shaNewPassword = crypto.createHash('sha512').update(newPassword).digest('hex');

            return await this._databaseAccess.forceChangePassword(username, shaNewPassword);
        } catch (e) {
            this._logger.error(`Error force changing password: ${e.message}`);
            return false;
        }
    }

    /**
     * Handles the forgot password functionality
     * @param email Email address of the user requesting password reset
     * @returns Boolean indicating success or failure
     */
    public async forgotPassword(email: string): Promise<boolean> {
        try {
            if (!this.validateEmail(email)) {
                this._logger.error(`Invalid email format: ${email}`);
                return false;
            }

            // Check if the user exists with the given email
            const userName = await this._databaseAccess.findUserByEmail(email);
            
            if (!userName) {
                this._logger.error(`No user found with email: ${email}`);
                return false;
            }


            // Generate a reset token
            const timestamp = Date.now();
            const tokenData = userName + email + timestamp;
            const token = crypto.createHash('sha256').update(tokenData).digest('hex');

            // Store token in Redis with TTL of 5 minutes
            await this._redis.addWithTTL(
                RedisKeys.AP_PASSWORD_RESET_TOKENS,
                [token, userName],
                this._dep.envConfig.resetTokenExpire);

            // Create reset password confirmation link
            let resetLink = `${this._dep.envConfig.resetPasswordLink}${token}`;

            const htmlTemplate = mailResetPassHTML(userName, resetLink)

            // Send email with reset password link
            const emailSent = await this._email.sendEmail(
                email,
                htmlTemplate
            )

            if (!emailSent) {
                this._logger.error(`Failed to send password reset email to: ${email}`);
                return false;
            }

            this._logger.info(`Password reset email sent to: ${email} for user: ${userName}`);
            return true;
        } catch (e) {
            this._logger.error(`Error in forgot password process: ${e.message}`);
            return false;
        }
    }

    /**
     * Resets a user's password using a token from a forgot password request
     * @param token Reset token to verify
     * @param newPassword New password to set
     * @returns Boolean indicating success or failure
     */
    public async resetPassword(token: string, newPassword: string): Promise<string | null> {
        try {
            if (!token || !newPassword) {
                this._logger.error('Missing required parameters for password reset');
                return null;
            }

            // Get token from redis using username as key
            const storedUserName = await this._redis.readField(RedisKeys.AP_PASSWORD_RESET_TOKENS, token);

            if (!storedUserName) {
                this._logger.error(`No user found for token: ${token}`);
                return null;
            }

            // Hash the new password
            const shaNewPassword = crypto.createHash('sha512').update(newPassword).digest('hex');

            // Update password using DatabaseAccess
            const success = await this._databaseAccess.resetPassword(storedUserName, shaNewPassword);

            if (!success) {
                this._logger.error(`Failed to update password for username: ${storedUserName}`);
                return null;
            }

            // Only remove token from Redis after successful database update
            await this._redis.remove(RedisKeys.AP_PASSWORD_RESET_TOKENS, [token]);

            this._logger.info(`Password reset successfully for username: ${storedUserName}`);
            return storedUserName;
        } catch (e) {
            this._logger.error(`Error resetting password: ${e.message}`);
            return null;
        }
    }

    /**
     * Validates email format
     * @param email Email to validate
     * @returns Boolean indicating if email format is valid
     */
    private validateEmail(email: string): boolean {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    private getEmailConfig(email: string, pass: string): EmailServiceConfig {
        return {
            host: 'smtp.gmail.com',
            port: 587,
            secure: false, // true for 465, false for other ports
            auth: {
                user: email,
                pass: pass,
            },
            from: email,
        }
    }
}

export default PasswordService;
