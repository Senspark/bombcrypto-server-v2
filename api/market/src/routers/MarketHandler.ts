import {Request, Response} from "express";
import {IDependencies} from "@project/Dependencies";
import MarketDatabaseAccess from "@project/services-impl/Market/MarketDatabaseAccess";
import MarketConfig from "@project/services-impl/Market/MarketConfig";
import MarketPlace from "@project/services-impl/Market/MarketPlace";
import {ILogger, IMarketConfig, IMarketDatabaseAccess, IMarketPlace} from "@project/Services";
import {randomResponse} from "@project/services-impl/Utils/RandomResponse";
import {MarketError} from "@project/consts/ErrorMessage";


const TAG = "[MarketHandler]";
export default class MarketHandler {

    private readonly _logger: ILogger
    private readonly _marketDatabase: IMarketDatabaseAccess
    private readonly _marketConfig: IMarketConfig;
    private readonly _marketPlace: IMarketPlace;
    private _isInitialize: boolean = false;

    constructor(private readonly _dep: IDependencies) {
        this._logger = _dep.logger.clone(TAG);
        this._marketDatabase = new MarketDatabaseAccess(_dep);
        this._marketConfig = new MarketConfig(_dep, this._marketDatabase);
        this._marketPlace = new MarketPlace(_dep, this._marketDatabase);
        this.Initialize().then()
    }

    private async Initialize(): Promise<void> {
        try {
            if(this._isInitialize) {
                this._logger.error("Market place already initialized");
                return;
            }
            await this._marketConfig.initialize();
            this._marketPlace.Initialize(this._marketConfig);

            this._isInitialize = true;
            this._logger.info("Market place initialized");

        } catch (error) {
            this._logger.error(`Failed to initialize market place: ${error}`);
            throw error;
        }
    }

    public async order(req: Request, res: Response) {

        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.orderItem(req, res);
        } catch (error) {
            this._logger.error(`Unhandled error in order request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async cancelOrder(req: Request, res: Response) {
        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.cancelOrder(req, res);
        } catch (error) {
            this._logger.error(`Unhandled error in cancelling order request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async buy(req: Request, res: Response) {
        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.buy(req, res);
        } catch (error) {
            this._logger.error(`Unhandled error in buy request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async sell(req: Request, res: Response) {
        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.sell(req, res);
        } catch (error) {
            this._logger.error(`Unhandled error in sell request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async edit(req: Request, res: Response) {
        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.edit(req, res);
        } catch (error) {
            this._logger.error(`Unhandled error in edit request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async cancel(req: Request, res: Response) {
        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.cancel(req, res);
        } catch (error) {
            this._logger.error(`Unhandled error in cancel request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getConfig(req: Request, res: Response) {
        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.getConfig(req, res);
        } catch (error) {
            this._logger.error(`Unhandled error in cancel request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getMyItem(req: Request, res: Response) {
        try {
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.getMyItemMarket(req, res)
        }
        catch (error) {
            this._logger.error(`Unhandled error in get my item request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    private async _verify(req: Request, res: Response): Promise<boolean> {
        if(!this._isInitialize){
            this._logger.error("Market place not initialized");
            res.sendError(MarketError.UNHANDLED_ERROR);
            return false;
        }

        // Check market is open
        if(!this._dep.envConfig.isMarketOpen){
            res.sendError(MarketError.MARKET_CLOSED);
            return false;
        }

        // Verify auth
        const isValid = await this._dep.bearerService.verifyBearer(req);
        if (!isValid) {
            this._logger.error("Invalid auth token");
            await randomResponse(res);
            return false;
        }
        return true;
    }



    // For test
    public async getSelling(req: Request, res: Response) {
        try{
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.getSelling(req, res);
        }
        catch (error) {
            this._logger.error(`Unhandled error in get selling request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getOrdering(req: Request, res: Response) {
        try{
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.getOrdering(req, res);
        }
        catch (error) {
            this._logger.error(`Unhandled error in get ordering request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getExpensive(req: Request, res: Response) {
        try{
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.getExpensive(req, res);
        }
        catch (error) {
            this._logger.error(`Unhandled error in get expensive request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

    public async getFixed(req: Request, res: Response) {
        try{
            if (!(await this._verify(req, res)))
                return;

            return this._marketPlace.getFixed(req, res);
        }
        catch (error) {
            this._logger.error(`Unhandled error in get expensive request: ${error}`);
            return res.sendError(MarketError.UNHANDLED_ERROR);
        }
    }

}