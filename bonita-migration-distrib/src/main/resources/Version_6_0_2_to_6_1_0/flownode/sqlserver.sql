--
-- arch_flownode_instance
-- 

ALTER TABLE arch_flownode_instance ADD executedByDelegate NUMERIC(19,0)
GO


--
-- flownode_instance
-- 

ALTER TABLE flownode_instance ADD executedByDelegate NUMERIC(19,0)
GO