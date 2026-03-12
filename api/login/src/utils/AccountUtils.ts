import {UserAccount} from "../services-impl/DatabaseAccess";
import {DataForDapp} from "../services-impl/login/dapp/DappLoginService";

/**
 * Converts a UserAccount to DataForDapp format
 * @param account - The user account to convert
 * @returns DataForDapp object
 */
export function toDataForDapp(account: UserAccount): DataForDapp {
    if (!account) {
        throw new Error("Account doesn't exist");
    }
    let userName : string | null = account.userName;
    if(account.isUserFi && userName === account.address){
        userName = null;
    }
    return {
        id: account.uid,
        username: userName,
        email: account.email || '',
        address: account.address,
        nickname: account.nickName || '',
        avatar: account.avatar || null,
    };
}