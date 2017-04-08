/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.core.impl.provider;

import org.everrest.core.impl.provider.multipart.CollectionMultipartFormDataMessageBodyWriter;
import org.everrest.core.impl.provider.multipart.ListMultipartFormDataMessageBodyReader;
import org.everrest.core.impl.provider.multipart.MapMultipartFormDataMessageBodyReader;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class ClientEmbeddedProvidersFeature implements Feature {
    @Override
    public boolean configure(FeatureContext context) {
        context.register(new ByteEntityProvider());
        context.register(new DataSourceEntityProvider());
        context.register(new DOMSourceEntityProvider());
        context.register(new FileEntityProvider());
        context.register(MultivaluedMapEntityProvider.class);
        context.register(new InputStreamEntityProvider());
        context.register(new ReaderEntityProvider());
        context.register(new SAXSourceEntityProvider());
        context.register(new StreamSourceEntityProvider());
        context.register(new StringEntityProvider());
        context.register(new StreamOutputEntityProvider());
        context.register(new JsonEntityProvider<>());
        context.register(JAXBElementEntityProvider.class);
        context.register(JAXBObjectEntityProvider.class);
        context.register(ListMultipartFormDataMessageBodyReader.class);
        context.register(MapMultipartFormDataMessageBodyReader.class);
        context.register(CollectionMultipartFormDataMessageBodyWriter.class);
        context.register(new JAXBContextResolver());
        context.register(new DefaultExceptionMapper());
        return true;
    }
}
