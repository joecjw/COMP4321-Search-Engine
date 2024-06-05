import * as React from "react";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";

export default function KeywordListSkeleton() {
	return (
		<Stack spacing={2}>
			<Skeleton variant="rounded" width={500} height={40} />
			<Skeleton variant="rounded" width={500} height={40} />
			<Skeleton variant="rounded" width={500} height={40} />
			<Skeleton variant="rounded" width={500} height={40} />
		</Stack>
	);
}
