import * as React from "react";
import PropTypes from "prop-types";
import Tabs from "@mui/material/Tabs";
import Tab from "@mui/material/Tab";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import CrawlerPanel from "./CrawlerPanel";
import SearchPanel from "./SearchPanel";
import PestControlIcon from "@mui/icons-material/PestControl";
import SearchIcon from "@mui/icons-material/Search";

function CustomTabPanel(props) {
	const { children, value, index, ...other } = props;

	return (
		<Box
			sx={{
				height: value !== index ? 0 : "100%",
				width: "75%",
				display: "flex",
				flexDirection: "column",
				alignItems: "center",
				justifyContent: "center",
			}}
			role="tabpanel"
			hidden={value !== index}
			id={`simple-tabpanel-${index}`}
			aria-labelledby={`simple-tab-${index}`}
			{...other}
		>
			{value === index && (
				<Box
					sx={{
						p: 3,
						minHeight: "100%",
						height: "100%",
						width: "100%",
						display: "flex",
						flexDirection: "column",
						alignItems: "center",
						justifyContent: "center",
					}}
				>
					<Typography
						sx={{
							minHeight: "100%",
							height: "100%",
							width: "100%",
						}}
						component={"div"}
					>
						{children}
					</Typography>
				</Box>
			)}
		</Box>
	);
}

CustomTabPanel.propTypes = {
	children: PropTypes.node,
	index: PropTypes.number.isRequired,
	value: PropTypes.number.isRequired,
};

function a11yProps(index) {
	return {
		id: `simple-tab-${index}`,
		"aria-controls": `simple-tabpanel-${index}`,
	};
}

export default function MainTabs() {
	const [value, setValue] = React.useState(0);

	const handleChange = (e, newValue) => {
		setValue(newValue);
	};

	return (
		<Box
			sx={{
				width: "100%",
				height: "100%",
				minWidth: "fit-content",
				minHeight: "fit-content",
				display: "flex",
				flexDirection: "column",
				alignItems: "center",
				justifyContent: "center",
			}}
		>
			<Box
				sx={{
					borderBottom: 2,
					borderColor: "divider",
					minWidth: "fit-content",
					width: "75%",
				}}
			>
				<Tabs value={value} onChange={handleChange} centered>
					<Tab
						label="Search"
						icon={<SearchIcon />}
						{...a11yProps(0)}
						sx={{ fontSize: "24px", minWidth: "50%" }}
					/>
					<Tab
						label="Crawler"
						icon={<PestControlIcon />}
						{...a11yProps(1)}
						sx={{ fontSize: "24px", minWidth: "50%" }}
					/>
				</Tabs>
			</Box>
			<CustomTabPanel value={value} index={0}>
				<SearchPanel />
			</CustomTabPanel>
			<CustomTabPanel value={value} index={1}>
				<CrawlerPanel />
			</CustomTabPanel>
		</Box>
	);
}
