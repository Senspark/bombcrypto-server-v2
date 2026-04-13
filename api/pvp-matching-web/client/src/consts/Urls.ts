import {getServerOrigin} from "../utils/EnvConfig";

const HOST = `${getServerOrigin()}/pvp`;

const Urls = {
    // @formatter:off
    FixtureGetRegisteredMatches : `${HOST}/tournament/registered`,
    FixtureRegisterMatchesGroup : `${HOST}/tournament/register-group`,
    FixtureUnregisterMatches    : `${HOST}/tournament/un-register`,

    GetRegisterTestMatch        : `${HOST}/test/registered`,
    RegisterTestMatch           : `${HOST}/test/register`,
    UnRegisterTestMatch         : `${HOST}/test/un-register`,
};

export default Urls;