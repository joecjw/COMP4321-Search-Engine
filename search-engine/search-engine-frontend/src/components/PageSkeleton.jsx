import * as React from "react";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";

export default function PageSkeleton() {
	return (
		<Stack spacing={1}>
			<Stack spacing={1} direction={"row"}>
				<Skeleton variant="rounded" width={90} height={70} />
				<Skeleton variant="rounded" width={520} height={90} />
			</Stack>
			<Skeleton variant="text" sx={{ fontSize: "1rem" }} />
			<Skeleton variant="text" sx={{ fontSize: "1rem" }} />
			<Skeleton variant="rounded" width={620} height={45} />
			<Skeleton variant="rounded" width={620} height={45} />
		</Stack>
	);
}
