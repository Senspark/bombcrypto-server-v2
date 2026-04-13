import "../pvp-fixture/Styles.css";
import {Dispatch, useState} from "react";
import {Alert, Button, message} from "antd";
import {Cmd} from "../pvp-fixture/FixtureMatchesPage";
import RegisterMatchTestModal from "./RegisterMatchTestModal";
import PvpTestController, {IRegisterMatchTestData} from "./PvpTestController";
import TestMatchPage from "./TestMatchPage";

const PvpTestDashboardPage = () => {
    const [registerMatchData] = useState<string[]>([]);
    const [resetSelectMatches] = useState<boolean>(false);
    const [messageApi, messageContextHolder] = message.useMessage();
    const [matchesCmd, setMatchesCmd] = useState<Dispatch<Cmd>>();
    const [registerMatchGroupModal, setRegisterMatchGroupModal] = useState<number>(0);

    const onRegisterMatchGroupCallback = async (data: IRegisterMatchTestData | null) => {
        if (data) {
            const result = await new PvpTestController().registerMatchTest(data);
            if (result === true) {
                matchesCmd?.(Cmd.Refresh);
                setRegisterMatchGroupModal(0);
            } else {
                messageApi.open({
                    type: "error",
                    content: result,
                });
                setRegisterMatchGroupModal(registerMatchGroupModal + 1);
            }
        } else {
            setRegisterMatchGroupModal(0);
        }
    }

    const handleDispatchCmdToMatches = (dispatch: Dispatch<Cmd>) => {
        setMatchesCmd(() => dispatch);
    };

    return (
        <div className={"standardPadding"}>
            {messageContextHolder}
            <Alert
                message="Quick PVP Test Setup"
                description="This tab allows you to quickly group two users for PVP testing or play with a bot on a specific server."
                type="info"
                showIcon
                style={{marginBottom: 24}}
            />
            <Button type={"primary"} onClick={() => setRegisterMatchGroupModal(1)}>Create Test Match</Button>
            <RegisterMatchTestModal state={registerMatchGroupModal} registerCallback={onRegisterMatchGroupCallback}/>
            <TestMatchPage registerMatchData={registerMatchData} resetSelection={resetSelectMatches}
                           dispatchCmd={handleDispatchCmdToMatches}/>
        </div>
    );
};

export default PvpTestDashboardPage;