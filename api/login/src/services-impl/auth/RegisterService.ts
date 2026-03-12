import {ILogger} from "../../Services";
import {IDependencies} from "../../DependenciesInjector";
import DatabaseAccess from "../../services-impl/DatabaseAccess";

export class RegisterService {
    constructor(
        private readonly _dep: IDependencies
    ) {
        this._logger = _dep.logger.clone('[REGISTER]');
        this._databaseAccess = _dep.generalServices.databaseAccess;
    }

    private readonly _logger: ILogger;
    private readonly _databaseAccess: DatabaseAccess;

    /**
     * Create a new Senspark account
     * @param userName Username for the new account
     * @param password Password for the new account
     * @param email Email for the new account
     * @returns User ID if account creation succeeds, null otherwise
     */
    public async createSensparkAccount(userName: string, password: string, email: string): Promise<number | null> {
        try {
            return await this._databaseAccess.createSensparkAccount(userName, password, email);
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
            return await this._databaseAccess.createAccountFi(userName, password, email, address);
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
            return await this._databaseAccess.createGuestAccount(userName);
        } catch (e) {
            this._logger.error(`Error creating guest account: ${e.message}`);
            return null;
        }
    }
}

export default RegisterService;
