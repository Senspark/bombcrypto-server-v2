import {IDatabaseManager, ILogger} from "../Services";
import crypto from "crypto";


export default class DatabaseAccess {
    constructor(
        private readonly _logger: ILogger,
        private readonly _database: IDatabaseManager
    ) {
        this._logger = _logger.clone('[DATABASE_ACCESS]');
    }

    // BSC/POLYGON - MOBILE - DAPP
    /**
     * Dùng cho lần đầu login, cần check thêm password để đảm bảo nhập đúng và lấy ra thông tin account
     * @param userName - The username of the account.
     * @param password - The password of the account.
     */
    public async checkAccountSenspark(userName: string, password: string): Promise<UserAccount | null> {
        const shaPassword = crypto.createHash('sha512').update(password).digest('hex');
        const sql = `SELECT *
                     FROM backend.bomberland.fn_select_user_account_web($1, $2)`;
        const values = [userName, shaPassword];
        return await this.queryUserAccount(sql, values);
    }

    /**
     * Check guest account with username
     * @param userName - The username of the account.
     */
    public async checkGuestAccount(userName: string): Promise<number | null> {
        try {
            const sql = `SELECT id
                         FROM backend.bomberland.users
                         WHERE username = $1
                           AND is_deleted = false`;
            const values = [userName];
            const result = await this._database.query(sql, values);

            if (result.rowCount != null && result.rowCount > 0) {
                return result.rows[0].id;
            } else {
                return null;
            }
        } catch (e) {
            this._logger.error(`Error checking user guest existence: ${e.message}`);
            return null;
        }
    }

    /**
     * Dùng khi đã login trước đó rồi, giờ đã có jwt nên chỉ cần dùng userName để lấy ra data account
     * @param userName - The username of the account.
     */
    public async getInfoAccountBsc(userName: string): Promise<UserAccount | null> {
        const sql = `SELECT *
                     FROM backend.bomberland.fn_select_user_account_web_no_password($1)`;
        const values = [userName];
        return await this.queryUserAccount(sql, values);
    }

    /**
     * Save wallet address to database or get existing account
     * @param walletAddress - The wallet address to save.
     */
    public async getOrCreateNewWalletBsc(walletAddress: string): Promise<UserAccount | null> {
        try {
            const sql = `SELECT *
                         FROM backend.bomberland.fn_select_or_insert_new_user_web($1)`;
            const values = [walletAddress];
            const result = await this._database.query(sql, values);
            if (result.rowCount === 0) {
                this._logger.error(`${walletAddress} failed to query user database`);
                return null;
            }
            const r = result.rows[0];
            return {
                uid: r.uid,
                userName: r.user_name,
                nickName: r.nick_name ?? null,
                email: null,
                address: r.address,
                typeAccount: 'FI',
                createAt: r.create_at,
                isUserFi: true
            };
        } catch (e) {
            this._logger.error(e.message);
            return null;
        }
    }

    /**
     * Check if a wallet account exists with given address and return account data
     * @param walletAddress - The wallet address to check
     * @returns UserAccount if account exists, null otherwise
     */
    public async getInfoAccountForDapp(walletAddress: string): Promise<UserAccount | null> {
        try {
            const sql = `
                SELECT id,
                       username as user_name,
                       nickname,
                       address,
                       email,
                       type_account,
                       avatar,
                       create_at
                FROM backend.bomberland.users
                WHERE (username = $1 OR address = $1)
                  AND is_deleted = false
                LIMIT 1
            `;
            const values = [walletAddress];
            const result = await this._database.query(sql, values);

            if (result.rowCount === null || result.rowCount === 0) {
                return null;
            }

            const r = result.rows[0];
            return {
                uid: r.uid,
                userName: r.user_name,
                nickName: r.nickname ?? null,
                email: r.email ?? null,
                address: r.address ?? null,
                typeAccount: r.type_account,
                createAt: r.create_at,
                isUserFi: r.type_account === "FI",
                avatar: r.avatar ?? null,
            };
        } catch (e) {
            this._logger.error(`Error checking wallet account: ${(e as Error).message}`);
            return null;
        }
    }

    /**
     * Query user account using SQL and values
     */
    private async queryUserAccount(sql: string, values: string[]): Promise<UserAccount | null> {
        try {
            const result = await this._database.query(sql, values);
            if (result.rowCount === 0) {
                this._logger.error(`Failed to query user database with values: ${values}`);
                return null;
            }
            const r = result.rows[0];
            return {
                uid: r.uid,
                userName: r.user_name,
                nickName: r.nickname ?? null,
                email: r.email ?? null,
                address: r.address,
                typeAccount: r.type_account,
                createAt: r.create_at,
                isUserFi: r.type_account === "FI"
            };
        } catch (e) {
            this._logger.error(e.message);
            return null;
        }
    }

    //SOL
    public async getOrCreateNewWalletSol(walletAddress: string): Promise<UserAccount | null> {
        try {
            const sql = `SELECT *
                         FROM backend.bomberland.fn_select_or_insert_new_user_sol($1)`;
            const values = [walletAddress];
            const result = await this._database.query(sql, values);
            if (result.rowCount === 0) {
                this._logger.error(`${walletAddress} failed to query user database`);
                return null;
            }
            const r = result.rows[0];
            return {
                uid: r.uid,
                userName: r.user_name,
                nickName: null,
                email: null,
                typeAccount: 'SOL',
                createAt: r.create_at,
                address: r.user_name,
                isUserFi: false
            };
        } catch (e) {
            this._logger.error(e.message);
            return null;
        }
    }


    //TON
    public async checkToUpdateTelegramId(initData: URLSearchParams, userName: string) {
        const userData = initData.get('user');
        if (userData) {
            const user = JSON.parse(userData);
            if (user && user.id) {
                const telegramId = user.id;

                this._logger.info(`Set Telegram id: ${telegramId} to user: ${userName}`);

                // Update telegram_id if it is NULL or empty
                const sql = `
                    UPDATE backend.bomberland.users
                    SET telegram_id = $1
                    WHERE username = $2
                      AND (telegram_id IS NULL OR telegram_id = '')
                    RETURNING telegram_id
                `;
                const values = [telegramId, userName];
                await this._database.query(sql, values);
            }
        }
    }

    public async getOrCreateNewWalletTon(walletAddress: string, telegramUserId: string | undefined, telegramUserName: string | undefined): Promise<TonAccount> {
        const email = undefined;
        const sql = `SELECT *
                     FROM backend.bomberland.fn_select_or_insert_new_user($1, $2, $3, $4, $5, $6)`;
        const values = [walletAddress, email, walletAddress, telegramUserName ?? null, 'FI', telegramUserId];
        const result = await this._database.query(sql, values);
        if (result.rowCount === 0) {
            throw new Error(`Failed to query user ${walletAddress} ${telegramUserId} ${telegramUserName}`);
        }
        return {
            uid: result.rows[0].uid,
            userName: result.rows[0].user_name,
            nickName: result.rows[0].nick_name,
            email: result.rows[0].email,
            typeAccount: result.rows[0].type_account,
            createAt: result.rows[0].create_at,
            telegramId: result.rows[0].telegram_id
        };
    }

    /**
     * Change password for a user by username
     * New password must be different from the current password
     * @param username Username of the account
     * @param currentPasswordHash Hashed current password for verification
     * @param newPasswordHash Hashed new password to set
     * @returns Boolean indicating success or failure
     */
    public async changePassword(username: string, currentPasswordHash: string, newPasswordHash: string): Promise<boolean> {
        try {
            const updateSql = `
                UPDATE backend.bomberland.users
                SET password  = $1,
                    update_at = NOW()
                WHERE username = $2
                  AND is_deleted = false
                  AND password = $3
                  AND password != $1
                RETURNING id
            `;

            const updateResult = await this._database.query(updateSql, [newPasswordHash, username, currentPasswordHash]);

            if (updateResult.rowCount === 0) {
                this._logger.error(`Failed to update password for username: ${username}. This could be because: user not found, incorrect current password, or new password is the same as the old one.`);
                return false;
            }

            this._logger.info(`Password updated successfully for username: ${username}`);
            return true;
        } catch (e) {
            this._logger.error(`Error changing password: ${e.message}`);
            return false;
        }
    }

    /**
     * Force change password for a user, this user not need to verify current password
     * New password must be different from the current password
     * @param username Username of the account
     * @param newPasswordHash Hashed new password to set
     * @returns Boolean indicating success or failure
     */
    public async forceChangePassword(username: string, newPasswordHash: string): Promise<boolean> {
        try {
            const updateSql = `
                UPDATE backend.bomberland.users
                SET password  = $1,
                    update_at = NOW()
                WHERE username = $2
                  AND is_deleted = false
                  AND password != $1
                RETURNING id
            `;

            const updateResult = await this._database.query(updateSql, [newPasswordHash, username]);

            if (updateResult.rowCount === 0) {
                this._logger.error(`User not found or failed to update password for username: ${username} or new password is the same as the old one.`);
                return false;
            }

            this._logger.info(`Password force updated successfully for username: ${username}`);
            return true;
        } catch (e) {
            this._logger.error(`Error force changing password: ${e.message}`);
            return false;
        }
    }

    /**
     * Find user by email for password reset functionality
     * @param email Email address of the user requesting password reset
     * @returns Username if found, null otherwise
     */
    public async findUserByEmail(email: string): Promise<string | null> {
        try {
            const findUserSql = `
                SELECT username
                FROM backend.bomberland.users
                WHERE email = $1
                  AND password IS NOT NULL
                  AND address IS NULL
                  AND is_deleted = false
                  AND type_account = 'TR'
            `;

            const userResult = await this._database.query(findUserSql, [email]);

            if (userResult.rowCount === 0) {
                this._logger.error(`No user found with email: ${email}`);
                return null;
            }

            return userResult.rows[0].username;
        } catch (e) {
            this._logger.error(`Error finding user by email: ${e.message}`);
            return null;
        }
    }

    /**
     * Reset a user's password using a token from a forgot password request
     * @param username Username whose password to reset
     * @param newPasswordHash Hashed new password to set
     * @returns Boolean indicating success or failure
     */
    public async resetPassword(username: string, newPasswordHash: string): Promise<boolean> {
        try {
            const updateSql = `
                UPDATE backend.bomberland.users
                SET password  = $1,
                    update_at = NOW()
                WHERE username = $2
                  AND is_deleted = false
                  AND type_account = 'TR'
                  AND address IS NULL
                RETURNING id
            `;

            const updateResult = await this._database.query(updateSql, [newPasswordHash, username]);

            if (updateResult.rowCount === 0) {
                this._logger.error(`Failed to update password for username: ${username}`);
                return false;
            }

            this._logger.info(`Password reset successfully for username: ${username}`);
            return true;
        } catch (e) {
            this._logger.error(`Error resetting password: ${e.message}`);
            return false;
        }
    }

    /**
     * Create a new Senspark account
     * @param userName Username for the new account
     * @param password Password for the new account
     * @param email Email for the new account
     * @returns User ID if account creation succeeds, null otherwise
     */
    public async createSensparkAccount(userName: string, password: string, email: string): Promise<number | null> {
        try {
            const shaPassword = crypto.createHash('sha512').update(password).digest('hex');
            const sql = `
                WITH existing_account AS (SELECT 1
                                          FROM backend.bomberland.users
                                          WHERE username = $1)
                INSERT
                INTO backend.bomberland.users (username, email, password, is_deleted, create_at, update_at,
                                               type_account)
                SELECT $1, $2, $3, false, NOW(), NOW(), 'TR'
                WHERE NOT EXISTS (SELECT 1 FROM existing_account)
                RETURNING id
            `;
            const values = [userName, email, shaPassword];
            const result = await this._database.query(sql, values);

            if (result.rowCount === 0) {
                this._logger.error(`Senspark account already exists or failed to create: ${userName}`);
                return null;
            }

            return result.rows[0].id;
        } catch (e) {
            this._logger.error(`Error creating senspark account: ${e.message}`);
            return null;
        }
    }

    /**
     * Create or update an account with Fi credentials
     * @param userName Username for the account
     * @param password Password for the account
     * @param email Email for the account
     * @param address BscWalletService address to associate with the account
     * @returns User ID if account update succeeds, null otherwise
     */
    public async createAccountFi(userName: string, password: string, email: string, address: string): Promise<number | null> {
        try {
            const shaPassword = crypto.createHash('sha512').update(password).digest('hex');
            const lowerCaseAddress = address.toLowerCase();
            
            const sql = `
                UPDATE backend.bomberland.users 
                SET username = $1, password = $2, email = $3, update_at = NOW()
                WHERE LOWER(address) = $4 AND is_deleted = false
                RETURNING id
            `;
            
            const values = [userName, shaPassword, email, lowerCaseAddress];
            const result = await this._database.query(sql, values);

            if (result.rowCount === 0) {
                this._logger.error(`Failed to update Fi account: address ${lowerCaseAddress} not found or is deleted`);
                return null;
            }

            return result.rows[0].id;
        } catch (e) {
            this._logger.error(`Error updating Fi account: ${e.message}`);
            return null;
        }
    }

    /**
     * Create a new Guest account
     * @param userName Username for the new guest account
     * @returns User ID if account creation succeeds, null otherwise
     */
    public async createGuestAccount(userName: string): Promise<number | null> {
        try {
            const sql = `INSERT INTO backend.bomberland.users (username, create_at, update_at, type_account)
                        VALUES ($1, NOW(), NOW(), 'GUEST')
                        RETURNING id`;
            const values = [userName];
            const result = await this._database.query(sql, values);

            if (result.rowCount === 0) {
                this._logger.error(`Failed to create guest account: ${userName}`);
                return null;
            }

            return result.rows[0].id;
        } catch (e) {
            this._logger.error(`Error creating guest account: ${e.message}`);
            return null;
        }
    }

    /**
     * Changes the nickname of a user
     * @param userName - The username or address of the user
     * @param newNickname - The new nickname to set
     * @returns Promise<boolean> - Whether the operation was successful
     */
    public async changeNickname(userName: string, newNickname: string): Promise<boolean> {
        try {
            const sql = `UPDATE backend.bomberland.users
                         SET nickname  = $1,
                             update_at = NOW()
                         WHERE username = $2
                            OR address = $2`;
            const values = [newNickname, userName];
            const result = await this._database.query(sql, values);

            if (result.rowCount === 0) {
                this._logger.error(`Failed to update nickname for user: ${userName}`);
                return false;
            }

            this._logger.info(`Nickname updated successfully for user: ${userName}`);
            return true;
        } catch (e) {
            this._logger.error(`Error updating nickname for user ${userName}: ${e.message}`);
            return false;
        }
    }

    /**
     * Changes a user's account from TR type to FI type with wallet address
     * @param userNameTr - The username of the TR account
     * @param walletAddress - The wallet address to associate with the account
     * @returns Promise<boolean> - Whether the operation was successful
     */
    public async changeUserTrToFi(userNameTr: string, walletAddress: string): Promise<boolean> {
        try {
            const sql = `UPDATE backend.bomberland.users
                         SET type_account = 'FI',
                             address      = $1,
                             update_at    = NOW()
                         WHERE username = $2
                           AND is_deleted = false
                           AND address IS NULL
                           AND type_account = 'TR'`;
            const values = [walletAddress, userNameTr];
            const result = await this._database.query(sql, values);

            if (result.rowCount === 0) {
                this._logger.error(`Failed to update username for user: ${walletAddress}`);
                return false;
            }

            this._logger.info(`Username updated successfully for user: ${walletAddress}`);
            return true;
        } catch (e) {
            this._logger.error(`Error updating username for user ${walletAddress}: ${e.message}`);
            return false;
        }
    }

    /**
     * Sets the avatar for a user
     * Make sure only change one row in db
     * @param userName - The username or address of the user
     * @param avatar - The avatar ID as a number
     * @returns Promise<boolean> - Whether the operation was successful
     */
    public async setAvatar(userName: string, avatar: number): Promise<boolean> {
        try {
            const sql = `UPDATE backend.bomberland.users
                         SET avatar    = $1,
                             update_at = NOW()
                         WHERE id IN (SELECT id
                                      FROM backend.bomberland.users
                                      WHERE (username = $2 OR address = $2)
                                        AND is_deleted = false
                                      LIMIT 1)`;
            const values = [avatar, userName];
            const result = await this._database.query(sql, values);

            if (result.rowCount === 0) {
                this._logger.error(`Failed to update avatar for user: ${userName}`);
                return false;
            }

            this._logger.info(`Avatar updated successfully for user: ${userName} to ${avatar}`);
            return true;
        } catch (e) {
            this._logger.error(`Error updating avatar for user ${userName}: ${e.message}`);
            return false;
        }
    }

    /**
     * Get user ID by username or address
     * @param userName - The username or address of the user
     * @returns Promise<number | null> - User ID if found, null otherwise
     */
    public async getUserIdByUsername(userName: string): Promise<number | null> {
        try {
            const userSql = `SELECT id
                             FROM backend.bomberland.users
                             WHERE username = $1
                                OR address = $1
                             LIMIT 1`;
            const userResult = await this._database.query(userSql, [userName]);

            if (userResult.rowCount === 0) {
                this._logger.error(`User not found: ${userName}`);
                return null;
            }

            return userResult.rows[0].id;
        } catch (e) {
            this._logger.error(`Error getting user ID for ${userName}: ${e.message}`);
            return null;
        }
    }

    /**
     * Get character data for a user in the game
     * @param userId - The user ID
     * @param typeNum - The numeric type corresponding to account type (0=FI, 1=TRIAL, 2=TR)
     * @param databaseBombcrypto - The database manager for Bombcrypto database
     * @returns Promise<{charactor: number}[]> - Raw array of character records from the database
     */
    public async getCharactersByUserIdAndType(userId: number, typeNum: number, databaseBombcrypto: IDatabaseManager): Promise<{charactor: number}[]> {
        try {
            const bomberSql = `SELECT charactor
                               FROM bombcrypto2.public.user_bomber
                               WHERE uid = $1
                                 AND type = $2`;
            const bomberValues = [userId, typeNum];
            const result = await databaseBombcrypto.query(bomberSql, bomberValues);

            if (result.rowCount === 0) {
                this._logger.error(`No character found for user ID: ${userId} with type: ${typeNum}`);
                return [];
            }

            return result.rows;
        } catch (e) {
            this._logger.error(`Error retrieving hero characters for user ID ${userId}: ${e.message}`);
            return [];
        }
    }

    /**
     * Check if a wallet can be assigned to a TR account
     * @param userName Username of the account
     * @param walletAddress Wallet address to assign
     * @returns Promise with object containing {canAssign: boolean, errorMessage?: string}
     */
    public async checkCanAssignWalletToAccount(userName: string, walletAddress: string): Promise<{canAssign: boolean, errorMessage?: string}> {
        try {
            // Check if the user exists with null address and account type 'TR'
            const userQuery = `SELECT *
                               FROM backend.bomberland.users
                               WHERE userName = $1
                                 AND address IS NULL
                                 AND type_account = 'TR'
                                 AND is_deleted = false
                               LIMIT 1`;
            const userResults = await this._database.query(userQuery, [userName]);

            if (!userResults || userResults.rowCount === 0) {
                this._logger.error(`${userName} - User not found or not eligible (must have null address and account type 'TR')`);
                return {
                    canAssign: false,
                    errorMessage: "User already has wallet address or not a TR account"
                };
            }

            // Check if the wallet address is already assigned to another user
            const addressQuery = `SELECT id
                                  FROM backend.bomberland.users
                                  WHERE address = $1
                                     OR username = $1
                                      AND is_deleted = false
                                  LIMIT 1`;
            const addressResults = await this._database.query(addressQuery, [walletAddress.toLowerCase()]);

            if (addressResults && addressResults.rowCount && addressResults.rowCount > 0) {
                this._logger.error(`${userName} - Wallet address ${walletAddress} is already assigned to another user`);
                return {
                    canAssign: false,
                    errorMessage: "This wallet address has been used"
                };
            }

            this._logger.info(`${userName} - Can assign wallet address ${walletAddress}`);
            return {
                canAssign: true
            };
        } catch (e) {
            this._logger.error(`Error checking wallet assignment eligibility: ${(e as Error).message}`);
            return {
                canAssign: false,
                errorMessage: "Cannot assign wallet to account"
            };
        }
    }

    /**
     * Get username by wallet address
     * @param userName - The wallet address to look up
     * @returns Promise<string | null> - Username if found, null otherwise
     */
    public async getUsernameByAddress(userName: string): Promise<string> {
        try {
            const sql = `
            SELECT username
            FROM backend.bomberland.users
            WHERE address = $1
              AND is_deleted = false
            LIMIT 1
        `;

            const result = await this._database.query(sql, [userName]);

            if (result.rowCount === 0) {
                this._logger.error(`No user found with address: ${userName}`);
                return "";
            }

            return result.rows[0].username;
        } catch (e) {
            this._logger.error(`Error getting username by address ${userName}: ${(e as Error).message}`);
            return "";
        }
    }
}


export type UserAccount = {
    uid: number;
    userName: string;
    nickName: string | null;
    email: string | null;
    typeAccount: string;
    createAt: Date;
    address: string;
    isUserFi: boolean;
    avatar?: number | null;
}

export interface TonAccount {
    uid: number;
    userName: string;
    nickName: string;
    email: string | undefined;
    typeAccount: string;
    createAt: Date;
    telegramId: string | undefined;
}
