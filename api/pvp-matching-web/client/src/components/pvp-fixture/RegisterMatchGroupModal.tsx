import {Alert, Modal, Radio, RadioChangeEvent} from "antd";
import TextArea from "antd/es/input/TextArea";
import {useEffect, useState} from "react";
import {IRegisterMatchGroupData, IRegisterPlayersGroup} from "./PvpFixtureController";
import {openExampleExcelDemo} from "./ExampleExcelDemo";

type Props = {
    state: number;
    registerCallback: (data: IRegisterMatchGroupData | null) => void;
};

const ID_TXT_INPUT_DATA = "txtInputData";

/**
 * This helps to arrange matches faster
 */
const RegisterMatchGroupModal = (props: Props) => {
    const openModal = props.state > 0;

    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrMsg] = useState<string | undefined>(undefined);
    const [modeChoose, setModeChoose] = useState(16);
    const [timeChoose, setTimeChoose] = useState<TimeMode>('now');
    const [heroProfile, setHeroProfile] = useState(1);
    const [serverChoose, setServerChoose] = useState("sg");

    useEffect(() => {
        setLoading(false);
    }, [props.state]);

    const handleOk = async () => {
        setErrMsg(undefined);
        const txtInputData = document.getElementById(ID_TXT_INPUT_DATA) as HTMLTextAreaElement;
        const value = txtInputData.value;
        if (!value || value.length === 0) {
            setErrMsg("Data must not be empty");
            return;
        }
        const data: IRegisterMatchGroupData = {
            usersData: parseData(value, timeChoose),
            heroProfile: heroProfile,
            fixedZone: serverChoose,
            mode: modeChoose,
        };
        setLoading(true);
        props.registerCallback(data);
    };

    const handleCancel = () => {
        props.registerCallback(null);
    };

    const onModeChooseChange = (e: RadioChangeEvent) => {
        setModeChoose(e.target.value);
    };

    const onTimeChooseChange = (e: RadioChangeEvent) => {
        setTimeChoose(e.target.value);
    };

    const onHeroProfileChange = (e: RadioChangeEvent) => {
        setHeroProfile(e.target.value);
    };

    const onServerChooseChange = (e: RadioChangeEvent) => {
        setServerChoose(e.target.value);
    };

    const errMsgView = errorMsg ? <Alert message={errorMsg} type="error"/> : null;

    if (openModal) {
        return (
            <Modal title="Register Participants" open={openModal} onOk={handleOk} confirmLoading={loading}
                   onCancel={handleCancel}>
                <div style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8}}>
                    <span>Paste the list of wallets copied from Excel</span>
                    <button type="button" style={{padding: '2px 8px', fontSize: '0.95em', cursor: 'pointer'}}
                            onClick={openExampleExcelDemo}>
                        Show Example Data
                    </button>
                </div>
                {errMsgView}
                <TextArea id={ID_TXT_INPUT_DATA} rows={4}/>

                <h3>Time:</h3>
                <Radio.Group onChange={onTimeChooseChange} value={timeChoose}>
                    <Radio value='now'>Today</Radio>
                    <Radio value='tomorrow'>Tomorrow</Radio>
                </Radio.Group>

                <h3>Game Mode:</h3>
                <Radio.Group onChange={onModeChooseChange} value={modeChoose}>
                    <Radio value={16}>BO3 Tournament</Radio>
                    <Radio value={32}>BO5 Tournament</Radio>
                    <Radio value={64}>BO7 Tournament</Radio>
                    <Radio value={1}>PvP Normal</Radio>
                </Radio.Group>

                <h3>Hero Profile</h3>
                <Radio.Group onChange={onHeroProfileChange} value={heroProfile}>
                    <Radio value={1}>Qualifier</Radio>
                    <Radio value={2}>Top 16+</Radio>
                    <Radio value={3}>Free</Radio>
                </Radio.Group>

                <h3>Server:</h3>
                <Radio.Group onChange={onServerChooseChange} value={serverChoose}>
                    <Radio value={"sg"}>Singapore</Radio>
                    <Radio value={"br"}>Brazil</Radio>
                    <Radio value={"de"}>Germany</Radio>
                    <Radio value={"jp"}>Japan</Radio>
                    <Radio value={"us"}>USA</Radio>
                </Radio.Group>
            </Modal>
        );
    } else {
        return null;
    }
};

export default RegisterMatchGroupModal;

function parseData(data: string, timeMode: TimeMode): IRegisterPlayersGroup[] {
    // Depending on the format of the Excel file for quick match creation
    const lines = data.split("\n");
    if (lines.length === 0) {
        return [];
    }
    const result: IRegisterPlayersGroup[] = [];
    for (const line of lines) {
        if (line.trim().length === 0) {
            continue;
        }
        const parts = line.split("\t");
        let userName1 = "";
        let userName2 = "";
        let timeUtc7 = "";
        if (parts.length === 3) {
            [userName1, timeUtc7, userName2] = parts;
        } else {
            console.error("Invalid data: " + line);
            continue;
        }
        const today = new Date();
        if (timeMode == 'tomorrow') {
            today.setDate(today.getDate() + 1);
        }
        let [from, to] = timeUtc7.split('-');
        const fromTime = today.setHours(parseInt(from.split(':')[0]), parseInt(from.split(':')[1]), 0, 0);
        const toTime = today.setHours(parseInt(to.split(':')[0]), parseInt(to.split(':')[1]), 0, 0);

        result.push({
            userName1: userName1.trim(),
            userName2: userName2.trim(),
            fromTime: new Date(fromTime),
            toTime: new Date(toTime)
        } as IRegisterPlayersGroup);
    }
    console.log(result);
    return result;
}

type TimeMode = 'now' | 'tomorrow'