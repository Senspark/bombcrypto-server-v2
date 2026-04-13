import FixtureMatchesPage, {Cmd} from "./FixtureMatchesPage";
import "./Styles.css";
import {Dispatch, useState} from "react";
import PvpFixtureController, {IRegisterMatchGroupData} from "./PvpFixtureController";
import {Alert, Button, message} from "antd";
import RegisterMatchGroupModal from "./RegisterMatchGroupModal";

const PvpFixtureDashboardPage = () => {
    const [registerMatchData] = useState<string[]>([]);
    const [resetSelectMatches] = useState<boolean>(false);
    const [messageApi, messageContextHolder] = message.useMessage();
    const [matchesCmd, setMatchesCmd] = useState<Dispatch<Cmd>>();
    const [registerMatchGroupModal, setRegisterMatchGroupModal] = useState<number>(0);

    const onRegisterMatchGroupCallback = async (data: IRegisterMatchGroupData | null) => {
        if (data) {
            const result = await new PvpFixtureController().registerMatchGroup(data);
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
                message="Quick PVP Tounament Setup"
                description="This tab provides an interface for quickly setting up PVP matches for tournaments."
                type="info"
                showIcon
                style={{marginBottom: 24}}
            />
            <Button type={"primary"} onClick={() => setRegisterMatchGroupModal(1)}>Setup tournament matches</Button>
            <RegisterMatchGroupModal state={registerMatchGroupModal} registerCallback={onRegisterMatchGroupCallback}/>
            <FixtureMatchesPage registerMatchData={registerMatchData} resetSelection={resetSelectMatches}
                                dispatchCmd={handleDispatchCmdToMatches}/>
        </div>
    );
};

export default PvpFixtureDashboardPage;