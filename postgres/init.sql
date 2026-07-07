CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgvector;
CREATE EXTENSION IF NOT EXISTS zhparser;

CREATE TEXT SEARCH CONFIGURATION chinese_zh (PARSER = zhparser);

ALTER TEXT SEARCH CONFIGURATION chinese_zh
    ADD MAPPING FOR n,v,a,nc,nt,ni,ns,nts,nz,o,u,d,i,x,l,e,en,un,ud,ug,uj,uz,uv,wh,y,bg,df,g,h,j,k,m,q,r,s,t,w,z
    WITH simple;
