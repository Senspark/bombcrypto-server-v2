import {NavLink, Outlet, Route, Routes} from 'react-router-dom';
import {useState} from "react";
import {Menu, MenuProps} from "antd";
import Pages from "../../consts/Pages";
import PvpFixtureDashboardPage from "../pvp-fixture/PvpFixtureDashboardPage";

import PvpTestDashboardPage from "../pvpTest/PvpTestDashboardPage";

type MenuItem = Required<MenuProps>['items'][number];

const privateMenuItems: MenuItem[] = [
    {
        label: (<NavLink to={Pages.Tournament}>Tournament</NavLink>),
        key: Pages.Tournament,
    },
    {
        label: (<NavLink to={Pages.PvpTest}>PVP Test</NavLink>),
        key: Pages.PvpTest,
    },
];


const Layout = () => {
    const [current, setCurrent] = useState('home');

    const handleMenuItemClick: MenuProps['onClick'] = (e) => {
        setCurrent(e.key);
    };

    return (
        <>
            <Menu mode='horizontal' onClick={handleMenuItemClick}
                  items={privateMenuItems}
                  selectedKeys={[current]}/>
            <br/>
            <Outlet/>
        </>
    );
};

const MainNavigation = () => {
    return (
        <Routes>
            <Route path={Pages.Home} element={<Layout/>}>
                <Route path={Pages.Tournament} element={
                    <PvpFixtureDashboardPage/>
                }/>
                <Route path={Pages.PvpTest} element={
                    <PvpTestDashboardPage/>
                }/>

                <Route path="*" element={<PvpFixtureDashboardPage/>}/>
            </Route>
        </Routes>
    );
};

export default MainNavigation;