import * as React from "react";
import Box from "@mui/material/Box";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import LinkIcon from "@mui/icons-material/Link";
import { Button, Typography } from "@mui/material";

const UrlList = ({ urls }) => {
	if (urls.length <= 0) {
		return (
			<Button variant="text" sx={{ fontSize: "15px", fontWeight: "600" }}>
				No URLs
			</Button>
		);
	}
	return (
		<Box sx={{ width: "100%" }}>
			<List>
				{urls.map((url, index) => {
					return (
						<ListItem key={index} disablePadding>
							<ListItemButton
								component="a"
								href={url}
								sx={{
									margin: "5px",
									borderRadius: "5px",
									bgcolor: "whitesmoke",
								}}
							>
								<ListItemIcon>
									<LinkIcon />
								</ListItemIcon>
								<Typography
									sx={{
										fontWeight: "500",
										fontSize: "16px",
										overflow: "hidden",
										color: "#1976d2",
									}}
								>
									{url}
								</Typography>
							</ListItemButton>
						</ListItem>
					);
				})}
			</List>
		</Box>
	);
};

export default UrlList;
