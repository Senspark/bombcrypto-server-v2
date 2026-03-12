import {ILogger} from "../../Services";
import IDatabaseManager from "../../services/IDatabaseManager";
import {IDependencies} from "../../DependenciesInjector";
import DatabaseAccess from "../../services-impl/DatabaseAccess";

const HERO_TYPE = {
    "FI": 0,
    "TRIAL": 1,
    "TR": 2
};

export class ProfileService {
    constructor(
        private readonly _dep: IDependencies
    ) {
        this._logger = _dep.logger.clone('[PROFILE]');
        this._databaseAccess = _dep.generalServices.databaseAccess;
        this._databaseBombcrypto = _dep.databaseBombcrypto;
    }

    private readonly _logger: ILogger;
    private readonly _databaseAccess: DatabaseAccess;
    private readonly _databaseBombcrypto: IDatabaseManager;

    /**
     * Changes the nickname of a user
     * @param userName - The username or address of the user
     * @param newNickname - The new nickname to set
     * @returns Promise<boolean> - Whether the operation was successful
     */
    public async changeNickname(userName: string, newNickname: string): Promise<boolean> {
        try {
            return await this._databaseAccess.changeNickname(userName, newNickname);
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
            return await this._databaseAccess.changeUserTrToFi(userNameTr, walletAddress);
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
            return await this._databaseAccess.setAvatar(userName, avatar);
        } catch (e) {
            this._logger.error(`Error updating avatar for user ${userName}: ${e.message}`);
            return false;
        }
    }

    /**
     * Sets the avatar for a user with string to number conversion
     * @param userName - The username or address of the user
     * @param avatarStr - The avatar ID as a string, will be converted to number
     * @returns Promise<{success: boolean, error?: string}> - Result with success status and optional error
     */
    public async setAvatarFromString(userName: string, avatarStr: string): Promise<{success: boolean, error?: string}> {
        try {
            // Convert string avatar to a number for database storage
            const numericAvatarId = Number(avatarStr);
            if (isNaN(numericAvatarId)) {
                this._logger.error(`Invalid avatar format: ${avatarStr}`);
                return {success: false, error: 'Invalid avatar format'};
            }

            const success = await this.setAvatar(userName, numericAvatarId);
            return {success};
        } catch (e) {
            this._logger.error(`Error updating avatar for user ${userName}: ${e.message}`);
            return {success: false, error: e.message};
        }
    }

    /**
     * Get hero character type for a user
     * @param userName - The username of the user
     * @param type - The account type (FI, TRIAL, TR)
     * @returns Promise<number[]> - Array of character IDs
     */
    public async getAvatarDapp(userName: string, type: string): Promise<number[]> {
        try {
            const numericType = HERO_TYPE[type];
            if (numericType === undefined) {
                this._logger.error(`Invalid hero type: ${type}`);
                return [];
            }
            
            // First get the user ID
            const userId = await this._databaseAccess.getUserIdByUsername(userName);
            if (userId === null) {
                return [];
            }
            
            // Then get the raw character data from database
            const characterRows = await this._databaseAccess.getCharactersByUserIdAndType(userId, numericType, this._databaseBombcrypto);
            
            // Extract unique character IDs from the result and filter them
            const characters = Array.from(new Set(characterRows.map(row => row.charactor)))
                .filter(characterId => characterId <= 9);
                
            this._logger.info(`Retrieved ${characters.length} unique characters <= 9 for user ID: ${userId}`);
            return characters;
        } catch (e) {
            this._logger.error(`Error retrieving hero characters for user ${userName}: ${e.message}`);
            return [];
        }
    }

    /**
     * Validates and assigns wallet to account with all necessary checks
     * @param userName - The username of the account
     * @param walletAddress - The wallet address to assign
     * @returns Promise<{canAssign: boolean, success?: boolean, errorMessage?: string}> - Result with status and error message
     */
    public async AssignWalletToAccount(
        userName: string, 
        walletAddress: string
    ): Promise<boolean> {
        try {
            // Check if wallet can be assigned using database access service
            const result = await this._databaseAccess.checkCanAssignWalletToAccount(userName, walletAddress);
            
            if (!result.canAssign) {
                this._logger.error(`${userName} - ${walletAddress} - Cannot assign wallet: ${result.errorMessage}`);
                return false;
            }
            
            // If all validations pass, assign the wallet
            const isAssignSuccess = await this.changeUserTrToFi(userName, walletAddress);
            if (!isAssignSuccess) {
                this._logger.error(`${userName} - ${walletAddress} - fail to change user TR to FI`);
                return false;
            }
            
            return true;
        } catch (e) {
            this._logger.error(`Error in validateAndAssignWalletToAccount for user ${userName}: ${e.message}`);
            return false
        }
    }
}

export default ProfileService;
