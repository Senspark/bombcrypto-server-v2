import {Alert, Modal, Radio, RadioChangeEvent, Tooltip} from "antd";
import {InfoCircleOutlined} from "@ant-design/icons";
import TextArea from "antd/es/input/TextArea";
import {useEffect, useState} from "react";
import {IRegisterMatchTestData} from "./PvpTestController";

type Props = {
    state: number;
    registerCallback: (data: IRegisterMatchTestData | null) => void;
};

const ID_TXT_INPUT_DATA_1 = "txtInputData1";
const ID_TXT_INPUT_DATA_2 = "txtInputData2";

/**
 * This helps to arrange matches faster
 */
const RegisterMatchTestModal = (props: Props) => {
    const openModal = props.state > 0;

    const [loading, setLoading] = useState(false);
    const [errorMsg1, setErrMsg1] = useState<string | undefined>(undefined);
    const [errorMsg2, setErrMsg2] = useState<string | undefined>(undefined);
    const [serverChoose, setServerChoose] = useState("sg");
    const [playWithBot, setPlayWithBot] = useState(false);

    useEffect(() => {
        setLoading(false);
    }, [props.state]);

    const handleOk = async () => {
        ResetError();
        const txtInputData1 = document.getElementById(ID_TXT_INPUT_DATA_1) as HTMLTextAreaElement;
        const value1 = txtInputData1.value;
        if (!value1 || value1.length === 0) {
            setErrMsg1("Data must not be empty");
            return;
        }
        let value2 = "";
        if (playWithBot) {
            value2 = "bot";
        } else {
            const txtInputData2 = document.getElementById(ID_TXT_INPUT_DATA_2) as HTMLTextAreaElement;
            value2 = txtInputData2.value;
            if (!value2 || value2.length === 0) {
                setErrMsg2("Data must not be empty");
                return;
            }
        }
        const data: IRegisterMatchTestData = {
            user1: value1,
            user2: value2,
            zone: serverChoose,
            id: Date.now()
        };
        setLoading(true);
        props.registerCallback(data);
    };

    const handleCancel = () => {
        ResetError();
        props.registerCallback(null);
    };


    const onServerChooseChange = (e: RadioChangeEvent) => {
        setServerChoose(e.target.value);
    };

    const userInputTooltip = (
        <Tooltip
            title="Enter your in-game user name if you are using a Senspark account, or a wallet address if you are using a wallet account or a Senspark account already linked with a wallet.">
            <InfoCircleOutlined style={{marginLeft: 6, cursor: "pointer", color: "#1677ff"}}/>
        </Tooltip>
    );

    const errMsgView1 = errorMsg1 ? <Alert message={errorMsg1} type="error"/> : null;
    const errMsgView2 = errorMsg2 ? <Alert message={errorMsg2} type="error"/> : null;
    if (openModal) {
        return (
            <Modal title="Create Match" open={openModal} onOk={handleOk} confirmLoading={loading}
                   onCancel={handleCancel}>
                <h4>User 1 {userInputTooltip}</h4>
                {errMsgView1}
                <TextArea id={ID_TXT_INPUT_DATA_1} rows={1} placeholder="0xabc..."/>
                <h4>User 2 {userInputTooltip}</h4>
                <label style={{display: "block", marginBottom: 8}}>
                    <input
                        type="checkbox"
                        checked={playWithBot}
                        onChange={e => setPlayWithBot(e.target.checked)}
                        style={{marginRight: 8}}
                    />
                    Play against bot
                </label>
                {errMsgView2}
                <TextArea id={ID_TXT_INPUT_DATA_2} rows={1} disabled={playWithBot} placeholder="0xabc..."/>

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

    function ResetError() {
        setErrMsg1(undefined);
        setErrMsg2(undefined);
    }
};

export default RegisterMatchTestModal;
