package com.tkck.domain.document.service;

import com.tkck.domain.document.model.entity.DocumentQueryRewriteCommandEntity;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteResultEntity;

public interface IQueryRewriteService {

    DocumentQueryRewriteResultEntity rewrite(DocumentQueryRewriteCommandEntity command);
}
