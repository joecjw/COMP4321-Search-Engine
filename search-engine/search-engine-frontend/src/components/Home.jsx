import React from "react";
import MainTabs from "./MainTabs";
import { Box } from "@mui/material";

const Home = () => {
	return (
		<Box
			sx={{
				width: "100%",
				height: "100%",
				display: "flex",
				alignItems: "center",
				justifyContent: "center",
				minWidth: "fit-content",
				minHeight: "100%",
			}}
		>
			<MainTabs />
		</Box>
	);
};

export default Home;
