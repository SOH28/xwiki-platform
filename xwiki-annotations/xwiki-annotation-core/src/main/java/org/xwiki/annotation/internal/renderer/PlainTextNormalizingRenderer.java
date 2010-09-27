/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.annotation.internal.renderer;

import org.xwiki.annotation.content.ContentAlterer;
import org.xwiki.annotation.renderer.GeneratorEmptyBlockChainingListener;
import org.xwiki.annotation.renderer.LinkLabelGeneratorChainingListener;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.link.LinkLabelGenerator;

/**
 * Plain text renderer that renders the current document with normalized spaces.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component("normalizer-plain/1.0")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class PlainTextNormalizingRenderer extends AbstractChainingPrintRenderer implements Initializable
{
    /**
     * Space normalizer content alterer to clean the rendered texts.
     */
    @Requirement("space-normalizer")
    protected ContentAlterer textCleaner;

    /**
     * Plain text parser used to parse generated link labels.
     */
    @Requirement("plain/1.0")
    protected StreamParser plainTextParser;

    /**
     * Link label generator, used to generate labels for links with no label in this renderer. <br />
     * TODO: it would be nice if we could somehow get the very same link generator that the XHTML default renderer is
     * using, since we need the two to be fully synchronized and generate the same content.
     */
    @Requirement
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        ListenerChain chain = new ListenerChain();
        setListenerChain(chain);

        // chain'em all
        // Construct the listener chain in the right order. Listeners early in the chain are called before listeners
        // placed later in the chain.
        chain.addListener(this);

        // empty block listener is needed by the label generator
        chain.addListener(new GeneratorEmptyBlockChainingListener(chain));
        chain.addListener(new LinkLabelGeneratorChainingListener(linkLabelGenerator, plainTextParser, chain));
        chain.addListener(new PlainTextNormalizingChainingRenderer(textCleaner, chain));
    }
}
